庫存規格書
===
---

## 概述

庫存採用**餘額表 + 異動記錄 + 每日快照**三表設計：

- **Inventory**（餘額表）：記錄各儲位的即時庫存數量，供業務操作使用
- **InventoryTransaction**（異動記錄）：記錄每筆庫存異動的流水帳，供追溯查詢
- **InventoryDailySnapshot**（每日快照）：每日結算時保存庫存快照，供報表使用

支援批次、效期管理，配貨時依 FIFO（先進先出）原則分配。

---

## 儲位結構

```
營業所 (Branch)
├── 大庫 (locationCode = branchCode)
│   ├── qty: 可用數量
│   ├── keepQty: 寄庫數量
│   └── returnQty: 待退庫數量
│
└── 業務員儲位 (locationCode = S001, S002...)
    └── qty: 車存數量
```

### 儲位類型

| 類型 | 說明 | locationCode |
|------|------|--------------|
| WAREHOUSE | 大庫 | = branchCode（例如 1000） |
| CAR | 業務員車存 | 業務員儲位代碼（例如 S001） |

---

## 資料結構

> 欄位定義（名稱、型別、nullable）以 Entity 為準，個別欄位語意見其 Javadoc：
> - `src/main/java/com/agony/wmsallocation/entity/inventory/Inventory.java`
> - `src/main/java/com/agony/wmsallocation/entity/inventory/InventoryTransaction.java`
> - `src/main/java/com/agony/wmsallocation/entity/inventory/InventoryDailySnapshot.java`

結構重點（不隨欄位改名變動的部分）：

- **Inventory** 唯一鍵：(branchCode, locationCode, productCode, batchNo)
  - 大庫（WAREHOUSE）有 `keepQty`（業務員寄庫的貨）與 `returnQty`（待退回工廠的貨）；業務員儲位（CAR）這兩欄固定為 0
- **InventoryTransaction** 的 `qtyChange`/`keepQtyChange`/`returnQtyChange` 均帶正負號，以 `sourceDocType + sourceDocNo` 追溯來源
  - 寄庫（KEEP）、領回寄庫（KEEP_RETRIEVE）、退庫（RETURN）同時影響兩個儲位，一次業務操作產生 2 筆 transaction
- **InventoryDailySnapshot** 唯一鍵：(snapshotDate, branchCode, locationCode, productCode, batchNo)

### InventoryTransactionType（異動類型）

| 值 | 說明 | 對應單據 |
|----|------|---------|
| RECEIVE | 收貨入庫 | FDO |
| ALLOCATE | 配貨扣庫 | AO |
| PICK_UP | 業務員領貨 | SRO |
| SALES | 銷售出庫 | SDO (SALES) |
| CUSTOMER_RETURN | 客戶退貨 | SDO (RETURN) |
| KEEP | 寄庫 | SKR |
| KEEP_RETRIEVE | 領回寄庫 | SRO |
| RETURN | 退庫 | SRR |
| RETURN_SHIP | 銷退送出 | BRO |

### InventoryDailySnapshot（每日庫存快照）

---

## 庫存異動

### 異動時機

| 作業 | 單據 | 來源 | 目標 | 變化 |
|------|------|------|------|------|
| 收貨 | FDO | - | 大庫 | qty + |
| 配貨 | AO | 大庫 | - | qty - |
| 領貨 | SRO | - | 業務員 | qty + |
| 銷售 | SDO (SALES) | 業務員 | - | qty - |
| 客戶退貨 | SDO (RETURN) | - | 業務員 | qty + |
| 寄庫 | SKR | 業務員 | 大庫 | 業務員 qty -, 大庫 keepQty + |
| 領回寄庫 | SRO | 大庫 | 業務員 | 大庫 keepQty -, 業務員 qty + |
| 退庫 | SRR | 業務員 | 大庫 | 業務員 qty -, 大庫 returnQty + |
| 銷退送出 | BRO | 大庫 | - | returnQty - |

### 異動流程圖

```
FDO 收貨
    │
    ▼
大庫 (qty)
    │
    ├── AO 配貨 ──► SRO 領貨 ──► 業務員 (qty)
    │                               │
    │                               ├── SDO 銷售 ──► 客戶
    │                               │
    │                               ├── SDO 退貨 ◄── 客戶
    │                               │
    │                               ├── SKR 寄庫 ──► 大庫 (keepQty)
    │                               │                    │
    │                               │               SRO 領回 ◄─┘
    │                               │
    │                               └── SRR 退庫 ──► 大庫 (returnQty)
    │                                                    │
    │                                               BRO 送出 ──► 工廠
```

### 異動與 Transaction 記錄對照

每次異動同時做兩件事：更新 Inventory 餘額 + 新增 InventoryTransaction 記錄。

| 作業 | Transaction 筆數 | qtyChange | keepQtyChange | returnQtyChange |
|------|-----------------|-----------|---------------|-----------------|
| 收貨 | 1 筆（大庫） | + | 0 | 0 |
| 配貨 | 1 筆（大庫） | - | 0 | 0 |
| 領貨 | 1 筆（業務員） | + | 0 | 0 |
| 銷售 | 1 筆（業務員） | - | 0 | 0 |
| 客戶退貨 | 1 筆（業務員） | + | 0 | 0 |
| 寄庫 | 2 筆 | 業務員 qty - / 大庫 0 | 0 / 大庫 + | 0 / 0 |
| 領回寄庫 | 2 筆 | 0 / 業務員 + | 大庫 - / 0 | 0 / 0 |
| 退庫 | 2 筆 | 業務員 - / 0 | 0 / 0 | 0 / 大庫 + |
| 銷退送出 | 1 筆（大庫） | 0 | 0 | - |

---

## 每日快照機制

### 觸發方式

1. **自動排程**：每日 23:59 由 `@Scheduled` 定時任務自動執行
2. **手動觸發**：透過 `POST /api/inventory/snapshot` 手動執行

### 執行邏輯

將 Inventory 表的所有記錄複製一份到 InventoryDailySnapshot，標記當天日期。
若當天已有快照，則覆蓋（避免重複執行產生重複資料）。

### 用途

- 查詢歷史庫存：「某營業所在 6/1 的庫存是多少？」
- 報表：月報、週報直接查快照表
- 交叉驗證：快照餘額 vs 異動記錄 SUM，可偵測資料不一致

---

## FIFO 配貨規則

配貨時依以下順序分配：

1. **效期優先**：效期近的先出（FEFO），所有業務員相同
2. **業務員優先度**：等級高的先分配——**只決定缺貨時誰先被滿足**，不影響拿到的批次品質（2026-07-09 定案）
3. **批次號**：相同效期時，批號小的先出

```sql
ORDER BY expiryDate ASC, batchNo ASC
```

---

## API 設計

> 端點定義（路徑、HTTP method、請求/回應格式）以 Controller 為準，或由 Swagger/OpenAPI 自動產生。

對外提供的能力：

- 查詢庫存清單（大庫、儲位、產品維度）
- 手動觸發當日快照

---

## 設計考量

### 為何採用「餘額表 + 異動記錄 + 每日快照」？

| 方案 | 優點 | 缺點 |
|------|------|------|
| 純餘額表 | 查詢快、簡單 | 無法追溯、無歷史 |
| 純流水帳 | 完美追溯 | 查詢慢 |
| **餘額表 + 異動記錄 + 快照 ✓** | **查詢快、可追溯、有歷史** | **寫入時維護三表** |

- **Inventory**：即時查詢，O(1) 取得當下庫存
- **InventoryTransaction**：追溯每筆異動來源單據
- **InventoryDailySnapshot**：快速查詢歷史庫存，供報表使用

### 為何分 qty / keepQty / returnQty？

- **qty**：可配貨、可銷售的正常庫存
- **keepQty**：業務員寄庫的貨，隔天要領回，不能配給別人
- **returnQty**：待退回工廠的報廢品，不能使用

### 批次與效期設計

> 本段記錄批次（batchNo）/ 效期（expiryDate）的設計取捨，供日後回顧「為什麼欄位長這樣」。

**批次目前的用途：僅 FIFO（近效期先出）。**

實務上這個排序是 **FEFO（First-Expired-First-Out）**——目標是「不要讓貨爛在架上」，排序鍵語意上就是效期。規格雖沿用「FIFO」一詞，但 `ORDER BY` 依 `expiryDate` 排。

因此，刻意**不**做以下設計（YAGNI，目前用不到）：

- **不設批次主檔（`ProductBatch`）**：批次目前只是「用來區分同商品、不同效期的標籤」，沒有需要集中管理的批次屬性。
- **不存製造日（`manufactureDate`）**：FIFO 排序看效期不看製造日，製造日加了也用不到。

**為何三表都帶 `expiryDate`？**

| 表 | 角色 | 理由 |
|----|------|------|
| InventoryTransaction | ✅ 正確 | 流水帳記錄「異動當下的事實」，應自我完備，不為渲染歷史而 join 可變主檔 |
| InventoryDailySnapshot | ✅ 正確 | 報表用的當時切片，同樣應自我完備 |
| Inventory（餘額表） | ⚠️ 刻意的 denormalization | `expiryDate` 對 `(productCode, batchNo)` 是函數相依，本質是批次屬性；因不設批次主檔，Inventory 是它唯一也合理的落腳點 |

> **Service 必須守的一致性規則**：同一個 `(productCode, batchNo)` 的所有 Inventory 列，必須共用同一個 `expiryDate`。schema 無法保證（expiryDate 不在唯一鍵內），只能靠程式碼維護。

**為何 FIFO 排序鍵是 `expiryDate` 而非 `batchNo`？**

不能只用 `batchNo` 排序取代 `expiryDate`，因為「batchNo 順序 == 效期順序」這個前提會破：

1. **多工廠**：`ProductFactory` 允許一商品多工廠生產，batchNo 由各廠自訂，跨廠之間沒有共同順序。
2. **保存期限假設會破**：改配方 / 換包裝 / 法規調整都會讓同商品的效期規則變動，屆時 batchNo 與效期脫鉤，FIFO 會 silently 排錯。
3. **字串排序脆弱**：batchNo 是供應商給的識別字串，字串排序易出錯（如 `"A100" < "A99"`）。

`batchNo` 在排序中的正當角色是**次鍵（tiebreaker）**：同效期時用來決定先後，讓結果穩定可重現。

**未來擴充路徑（additive，不打掉現有資料）：**

- **追溯 / 召回**：骨架已在 `InventoryTransaction`（`batchNo` + `sourceDocType` + `sourceDocNo`），查詢即可串出批次流向，**不需批次主檔**。
- **品質管理 / 製造日 / 檢驗報告**：屬於批次屬性，屆時才拉 `ProductBatch` 主檔，`(productCode, batchNo)` 升級為主檔鍵，三表的 `(productCode, batchNo)` 改為參照。

**現在要守的兩條紀律（讓未來便宜）：**

1. `batchNo` 全程不變、一路帶著（收貨→配貨→領貨→銷售→Transaction），追溯鏈才完整。
2. `batchNo` 是識別、`expiryDate` 是獨立欄位——不從 batchNo 字串 parse 出效期。

---

## 相關規格書

- [產品主檔規格書](./Product.md)
- [配貨單規格書](../allocation/AllocationOrder.md)
- [業務員寄庫單規格書](../closing/SalesKeepRecord.md)
- [業務員退庫單規格書](../closing/SalesReturnRecord.md)

---
