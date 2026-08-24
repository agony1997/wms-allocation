工廠出貨單 (FDO)
===
---

## 概述

工廠根據營業所訂貨單 (BPO) 出貨後產生的單據。
營業所庫務人員收貨時，需核對數量、批次、效期，確認後貨品入庫。

---

## 單據來源

### 實際情境
- 工廠系統根據 BPO 產生 FDO
- FDO 隨貨送達營業所

### Mock 模擬方式
- **手動觸發**：提供獨立的工廠模擬頁面，由操作者選擇待出貨的 BPO，手動觸發產生 FDO（模擬工廠出貨）

---

## 單據狀態

| 狀態 | 說明 |
|------|------|
| PENDING | 待收貨（工廠已出貨，營業所尚未收貨） |
| RECEIVED | 已收貨（庫務已確認收貨） |
| DISCREPANCY | 有差異（收貨數量與出貨數量不符） |

---

## 資料結構

> 欄位定義（名稱、型別、nullable）一律以 Entity 為準，個別欄位語意見其 Javadoc：
> - 單頭：`src/main/java/com/agony/wmsallocation/entity/receive/FactoryDeliveryOrder.java`
> - 明細：`src/main/java/com/agony/wmsallocation/entity/receive/FactoryDeliveryOrderDetail.java`

結構重點（不隨欄位改名變動的部分）：

- FDO 對應一張上游**營業所訂貨單 (BPO)**，並標記**出貨工廠**
- 明細逐項記錄產品、批次、效期、**出貨數量**與**實收數量**
- 收貨確認後依**實收數量**入庫（見下方「庫存影響」）

---

## 作業流程

### 1. 收貨作業

```
1. 庫務進入收貨頁面
2. 選擇/掃描 FDO 單號
3. 系統顯示 FDO 明細（產品、批次、效期、出貨數量）
4. 庫務逐項輸入實收數量
5. 系統比對出貨數量與實收數量
   - 相符：狀態設為 RECEIVED
   - 不符：狀態設為 DISCREPANCY，記錄差異
6. 確認收貨，貨品入庫
```

### 2. 差異處理

當實收數量與出貨數量不符時：
- 記錄差異數量（deliveryQty - receivedQty）
- FDO 狀態標記為 DISCREPANCY
- 差異原因可填寫於備註
- 後續可透過庫存調整單 (IAO) 處理

---

## 庫存影響

### 入庫規則
- 收貨確認後，依**實收數量**增加庫存
- 庫存記錄需包含：產品代碼、批次號、效期、數量
- 後續配貨時依 FEFO（近效期先出）原則取貨

### 庫存結構

庫存唯一鍵與批次／效期設計以[庫存規格書](../master/Inventory.md)為準（唯一鍵：branchCode, locationCode, productCode, batchNo）。

---

## API 設計

> 端點定義（路徑、HTTP method、請求/回應格式）以 Controller 為準，或由 Swagger/OpenAPI 自動產生。

對外提供的能力：

- 查詢待收貨清單
- 查詢收貨記錄（已收貨、有差異）
- 查詢單一 FDO 明細
- 確認收貨
- [Mock] 模擬工廠出貨

---

## 權限

| 角色 | 權限 |
|------|------|
| 庫務人員 | 收貨確認 |
| 營業所管理員 | 查詢收貨記錄 |
| 系統管理員 | Mock 模擬操作 |

---

## 相關單據

- 上游：營業所訂貨單 (BPO)
- 下游：庫存、配貨作業 (AO)

---
