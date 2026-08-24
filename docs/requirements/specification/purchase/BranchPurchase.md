營業所訂貨規格書
===
> 主要操作者 : 庫務，組長
---

營業所彙總所屬營業員的訂單後，統一向工廠端訂貨。
可在此時強迫調整各營務員的實際訂購數量。

只可以編輯、凍結和彙總後天的訂單。

---

## 作業流程

```
階段 1: 開放 (BPF 不存在)
├── 業務員可編輯 SPO
│
▼ 組長執行凍結
階段 2: 凍結 (BPF.status = FROZEN)
├── 業務員不可編輯
├── 組長可調整 confirmedQty
│
▼ 組長確認完成
階段 3: 確認 (BPF.status = CONFIRMED)
├── 組長不可再編輯
├── 等待庫務彙總
│
▼ 庫務建立 BPO
階段 4: 彙總完成
├── SPOD.status = AGGREGATED
└── BPO 送出給工廠
```

---

> ### 組長彙總頁面功能

1. 查詢（列出營業所下所有儲位的訂購）
2. 新增產品
3. 編輯每行產品之確認數量 (confirmedQty)
4. 儲存 (更新)

> ### 特殊功能

1. 凍結營業所訂單（業務員不可再修改）
2. 確認完成（組長也不可再修改）
3. 解除凍結（在確認前可解除）

---

> ### 表格欄位

- 產品代碼
- 產品名稱
- 單位
- 確認數量 (confirmedQty)
- 原始訂購數量 (qty)
- 增減數量 (confirmedQty - qty)
- 橫向動態展開的儲位欄位

---

> ### 搜尋欄位

當選項有多筆時，預設值按代碼ASC排序取第一筆。

> 營業所
- 必填
- 預設值: 有權限的營業所。

> 日期
- 必填
- 預設值: 後天

---

> ### 資料結構

> 欄位定義（名稱、型別、nullable）以 Entity 為準，個別欄位語意見其 Javadoc：
> - `src/main/java/com/agony/wmsallocation/entity/purchase/BranchPurchaseFrozen.java`
> - `src/main/java/com/agony/wmsallocation/entity/purchase/BranchPurchaseOrder.java`
> - `src/main/java/com/agony/wmsallocation/entity/purchase/BranchPurchaseOrderDetail.java`

結構重點（不隨欄位改名變動的部分）：

- BPF 唯一鍵：(branchCode, purchaseDate)；記錄凍結／確認的操作人員與時間
- BPO 依**工廠**分組，唯一鍵：(branchCode, factoryCode, purchaseDate)，同一天同一營業所可有多張
- BPOD 的 `qty` 為各 SPOD.confirmedQty 的加總
- SPOD 彙總後回填 `bpoNo`，記錄併入的 BPO（配貨查詢「對應 BPO 已收貨」的依據）
- **狀態約束（聚合／鏡像）**：BPO 本身擁有收貨狀態欄位，此欄位是 `FactoryDeliveryOrder` (FDO) 收貨狀態的**反正規化鏡像**。為了配貨查詢效能而落地。兩者同步點在 `FactoryDeliveryOrderService` 收貨確認時；除該路徑外，不應有其他修改 BPO 收貨狀態的管道（2026-07-21 定案）。

---

> ### BPF 狀態說明

| 狀態 | 說明 | 業務員 | 組長 |
|------|------|--------|------|
| (不存在) | 開放中 | ✅ 可編輯 | - |
| FROZEN | 已凍結 | ❌ 不可編輯 | ✅ 可調整 confirmedQty |
| CONFIRMED | 已確認 | ❌ 不可編輯 | ❌ 不可編輯 |

---

> ### 庫務彙總作業

庫務根據已確認 (BPF.status = CONFIRMED) 的 SPO 明細建立 BPO：

1. 查詢該營業所當天所有 SPOD (status = PENDING)
2. 依產品對應的工廠分組
3. 每個工廠產生一張 BPO
4. BPOD.qty = 各 SPOD.confirmedQty 加總
5. 更新 SPOD.status = AGGREGATED，並回填 `bpoNo`（記錄來源 BPO，供配貨查詢「對應 BPO 是否已收貨」）

```
SPO-1 ─┬─ SPOD (產品A, 工廠1) ──┐
       └─ SPOD (產品B, 工廠1) ──┼──► BPO-1 (工廠1)
SPO-2 ─┬─ SPOD (產品A, 工廠1) ──┘
       └─ SPOD (產品C, 工廠2) ──────► BPO-2 (工廠2)
```

---

> ### API 設計

> 端點定義（路徑、HTTP method、請求/回應格式）以 Controller 為準，或由 Swagger/OpenAPI 自動產生。

對外提供的能力：

- 查詢／更新營業所彙總資料（組長）
- 凍結／解除凍結／確認完成（組長）
- 執行彙總建立 BPO（庫務）
- 查詢 BPO 清單（庫務）

---

> ### 設計考量

#### 為何需要 BPF？

1. **凍結粒度為營業所+日期**：一次凍結整個營業所當天的所有 SPO
2. **快速查詢**：判斷是否可編輯只需查 BPF 一筆記錄，而非遍歷所有 SPO
3. **審計軌跡**：記錄誰在何時執行凍結/確認

#### 為何 BPO 按工廠分？

1. **業務事實**：不同產品來自不同工廠，需分別向各工廠訂貨
2. **後續流程**：工廠出貨 (FDO) 也是按工廠產生
3. **追蹤方便**：可獨立追蹤各工廠的訂貨狀態

#### 為何 SPOD 有狀態而非 SPO？

1. **狀態粒度正確**：同一 SPO 的明細可能分進不同 BPO（按工廠分）
2. **避免同步問題**：只維護一處狀態，不會不一致
3. **可追蹤進度**：知道哪些明細已彙總、哪些還沒

---

> ### 相關規格書

- [業務員訂貨規格書](./SalesPurchase.md)
- [工廠出貨單規格書](../receive/FactoryDeliveryOrder.md)
- [作業流程](../WorkFlow.md)

---
