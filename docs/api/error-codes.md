# API 錯誤碼對照表

> 本表是前後端的**單一真相來源（single source of truth）**。
> 後端在 `com.agony.wmsallocation.exception.ErrorCode` 新增錯誤碼時，**同步**在此補一列；前端依本表決定要對哪些碼做特別處理。

## 使用約定

- **失敗回應格式**：所有錯誤統一由 `GlobalExceptionHandler` 回傳 `ErrorResponse`，欄位為
  `httpStatusCode` / `message`（繁中，可改字） / `errorCode`（本表的 code，**契約，不可隨意改名**） / `timestamp` / `path`。
- **前端消費原則**：99% 的錯誤只需顯示後端回的 `message`；只有「需要做不一樣的 UI 行為」的少數碼才 `switch(errorCode)` 特別處理（見「前端是否分支」欄）。
- **破壞性變更**：`errorCode` 一旦對外送出即為契約。**重新命名既有 code = 破壞性變更**，須通知前端、不可當作無害 refactor。新增 code 不算破壞性。
- **多對一**：多個業務 code 可對應同一個 HTTP status（例如都用 `409`），這正是 `errorCode` 存在的意義——HTTP status 分不出來的，靠 code 分。

## 通用錯誤碼（跨領域共用）

| code | HTTP | 說明 | 觸發情境 | 前端是否分支 |
|------|------|------|---------|------------|
| `RESOURCE_NOT_FOUND` | 404 | 找不到指定資源 | 依 id / code 查詢或操作，目標不存在；或請求打到不存在的端點/路徑 | 否（看 404 + message 即可） |
| `VALIDATION_ERROR` | 400 | 欄位驗證失敗 | `@Valid` 檢查未通過（`message` 含各欄位錯誤）；或 Spring MVC 參數綁定失敗（缺少必要 `@RequestParam`、型別不符等）；或 `@Valid` 管不到的跨欄位形狀錯誤（如收貨明細 `itemNo` 重複、缺少實收數量） | 視情況（可標紅對應欄位） |
| `INTERNAL_SERVER_ERROR` | 500 | 未預期的系統錯誤 | 兜底，不屬於上述任何情境 | 否（顯示通用錯誤畫面） |

## 領域錯誤碼

> 跟著功能長：實作到某領域、需要讓前端區分某個業務錯誤時，才在此新增。尚無資料時保留標題即可。

### auth（登入 / 身分）

| code | HTTP | 說明 | 觸發情境 | 前端是否分支 |
|------|------|------|---------|------------|
| `AUTH_BAD_CREDENTIALS` | 401 | 帳號或密碼錯誤 | 登入時查無帳號、密碼不符、或帳號已停用（一律回相同訊息，不洩漏帳號是否存在） | 否（顯示 message 即可） |

### branch（營業所）

| code | HTTP | 說明 | 觸發情境 | 前端是否分支 |
|------|------|------|---------|------------|
| `BRANCH_CODE_DUPLICATED` | 409 | 營業所代碼重複 | 新增時 branchCode 已存在 | 是（標紅 branchCode 欄位） |
| `BRANCH_HAS_DEPENDENTS`  | 409 | 營業所尚有下轄資料 | 刪除時仍有儲位或人員下轄資料 | 是（提示無法刪除原因） |

### sales-org（銷售組織）

| code | HTTP | 說明 | 觸發情境 | 前端是否分支 |
|------|------|------|---------|------------|
| `SALES_ORG_CODE_DUPLICATED` | 409 | 銷售組織代碼重複 | 新增時 salesOrgCode 已存在 | 是（標紅 salesOrgCode 欄位） |
| `SALES_ORG_HAS_DEPENDENTS`  | 409 | 銷售組織尚有下轄資料 | 刪除時仍有營業所或客戶下轄資料 | 是（提示無法刪除原因） |

### customer（客戶）

| code | HTTP | 說明 | 觸發情境 | 前端是否分支 |
|------|------|------|---------|------------|
| `CUSTOMER_CODE_DUPLICATED` | 409 | 客戶代碼重複 | 新增時 customerCode 已存在 | 是（標紅 customerCode 欄位） |

### factory（工廠）

| code | HTTP | 說明 | 觸發情境 | 前端是否分支 |
|------|------|------|---------|------------|
| `FACTORY_CODE_DUPLICATED` | 409 | 工廠代碼重複 | 新增時 factoryCode 已存在 | 是（標紅 factoryCode 欄位） |

### product（商品）

| code | HTTP | 說明 | 觸發情境 | 前端是否分支 |
|------|------|------|---------|------------|
| `PRODUCT_CODE_DUPLICATED` | 409 | 商品代碼重複 | 新增時 productCode 已存在 | 是（標紅 productCode 欄位） |

### sequence（取號）

| code | HTTP | 說明 | 觸發情境 | 前端是否分支 |
|------|------|------|---------|------------|
| `SEQUENCE_OVERFLOW` | 409 | 單日序號溢號 | 某類型單日序號超過上限 999，無法再取號 | 否（顯示 message 即可） |

### purchase（訂貨）

| code | HTTP | 說明 | 觸發情境 | 前端是否分支 |
|------|------|------|---------|------------|
| `PURCHASE_DATE_OUT_OF_RANGE` | 400 | 訂貨日不在合法區間 | 儲存訂貨明細時 purchaseDate 不在 D+2 ~ D+9（以當下日期為基準） | 否（顯示 message 即可） |
| `PURCHASE_ORDER_NOT_EDITABLE` | 409 | 訂貨單不可編輯 | 儲存訂貨明細時該營業所當天已被 BPF 凍結（FROZEN/CONFIRMED） | 否（顯示 message 即可） |
| `PRODUCT_FACTORY_NOT_CONFIGURED` | 409 | 商品缺少工廠對應，無法彙總 | 執行 BPO 彙總時，有商品缺少 ProductFactory.isDefault 對應，或商品主檔查無資料 | 否（顯示 message 即可） |
| `PURCHASE_ORDER_NOT_FOUND` | 409 | 待配 SPOD 查無對應訂貨單 | 執行配貨時，待配 SPOD 的 purchaseNo 找不到對應 SalesPurchaseOrder（資料一致性異常，理論上彙總階段已保證存在） | 否（顯示 message 即可） |

### receive（收貨）

| code | HTTP | 說明 | 觸發情境 | 前端是否分支 |
|------|------|------|---------|------------|
| `FDO_ALREADY_SHIPPED` | 409 | 訂貨單已出貨 | Mock 工廠出貨時該 BPO 已產生過 FDO（一張 BPO 僅能出貨一次） | 否（顯示 message 即可） |
| `FDO_NOT_RECEIVABLE` | 409 | 工廠出貨單非待收貨狀態 | 收貨確認時該 FDO 已非 PENDING（已收貨或已有差異，不可重複確認） | 否（顯示 message 即可） |

### inventory（庫存）

| code | HTTP | 說明 | 觸發情境 | 前端是否分支 |
|------|------|------|---------|------------|
| `INVENTORY_INSUFFICIENT` | 409 | 庫存不足 | 配貨/銷售/寄庫/領回寄庫/退庫/銷退送出等扣庫操作，現有數量小於需求數量 | 否（顯示 message 即可） |

## 變更紀錄

| 日期 | 變更 | 對前端影響 |
|------|------|-----------|
| 2026-06-29 | 建立對照表，登錄通用碼 `RESOURCE_NOT_FOUND` / `VALIDATION_ERROR` / `INTERNAL_SERVER_ERROR` | 無（既有行為） |
| 2026-06-29 | branch 新增 `BRANCH_CODE_DUPLICATED`（409），對應 POST /api/branches 代碼重複 | 新增碼，可標紅 branchCode 欄位 |
| 2026-06-29 | branch 新增 `BRANCH_HAS_DEPENDENTS`（409），對應 DELETE /api/branches 仍有下轄資料（儲位/人員） | 新增碼，可提示無法刪除原因 |
| 2026-06-29 | sales-org 新增 `SALES_ORG_CODE_DUPLICATED`（409），對應 POST /api/sales-orgs 代碼重複 | 新增碼，可標紅 salesOrgCode 欄位 |
| 2026-06-29 | sales-org 新增 `SALES_ORG_HAS_DEPENDENTS`（409），對應 DELETE /api/sales-orgs 仍有下轄資料（營業所/客戶） | 新增碼，可提示無法刪除原因 |
| 2026-07-01 | customer 新增 `CUSTOMER_CODE_DUPLICATED`（409），對應 POST /api/customers 代碼重複 | 新增碼，可標紅 customerCode 欄位 |
| 2026-07-01 | factory 新增 `FACTORY_CODE_DUPLICATED`（409），對應 POST /api/factories 代碼重複 | 新增碼，可標紅 factoryCode 欄位 |
| 2026-07-01 | product 新增 `PRODUCT_CODE_DUPLICATED`（409），對應 POST /api/products 代碼重複 | 新增碼，可標紅 productCode 欄位 |
| 2026-07-02 | sequence 新增 `SEQUENCE_OVERFLOW`（409），單日序號超過上限 999 時拋出 | 新增碼，顯示 message 即可 |
| 2026-07-02 | purchase 新增 `PURCHASE_DATE_OUT_OF_RANGE`（400）、`PURCHASE_ORDER_NOT_EDITABLE`（409），對應 PUT /api/sales-purchase-orders 日期區間與凍結守門 | 新增碼，顯示 message 即可 |
| 2026-07-02 | `VALIDATION_ERROR` 觸發情境擴充：原本只由 `@Valid` 觸發，現同時涵蓋 Spring MVC 參數綁定失敗（缺 `@RequestParam`、型別不符），未新增 code | 無（既有碼，僅新增觸發途徑） |
| 2026-07-03 | auth 新增 `AUTH_BAD_CREDENTIALS`（401），對應 POST /api/auth/login 帳密錯誤／帳號停用 | 新增碼，顯示 message 即可 |
| 2026-07-03 | `RESOURCE_NOT_FOUND` 觸發情境擴充：打到不存在的路徑（`NoResourceFoundException`）改回 404，不再誤回 500，未新增 code | 無（既有碼，僅新增觸發途徑） |
| 2026-07-06 | purchase 新增 `PRODUCT_FACTORY_NOT_CONFIGURED`（409），對應 POST /api/branch-purchase-orders/actions/aggregate 商品缺工廠對應 | 新增碼，顯示 message 即可 |
| 2026-07-07 | receive 新增 `FDO_ALREADY_SHIPPED`（409），對應 POST /api/factory-delivery-orders/actions/ship 重複出貨 | 新增碼，顯示 message 即可 |
| 2026-07-08 | receive 新增 `FDO_NOT_RECEIVABLE`（409），對應 POST /api/factory-delivery-orders/actions/receive 非 PENDING 狀態不可收貨確認 | 新增碼，顯示 message 即可 |
| 2026-07-13 | inventory 新增 `INVENTORY_INSUFFICIENT`（409），`InventoryService` 扣庫類操作原拋 `IllegalStateException` 無 handler 誤回 500，改走 `BusinessException`；庫存記錄不存在改用既有 `ResourceNotFoundException`（不新增碼），原拋 `IllegalArgumentException` 同樣誤回 500 | 新增碼，顯示 message 即可 |
| 2026-07-15 | purchase 新增 `PURCHASE_ORDER_NOT_FOUND`（409），對應 `AllocationService.allocate()` 待配 SPOD 查無對應 SPO 時明確中止（取代原本 locationCode=null 導致 NOT NULL 例外整批 rollback） | 新增碼，顯示 message 即可 |
| 2026-08-26 | `VALIDATION_ERROR` 觸發情境擴充：`POST /api/factory-delivery-orders/actions/receive` 的明細 `itemNo` 重複，原先由 `Collectors.toMap` 拋 `IllegalStateException` 誤回 500，改為在碰 DB 前擋下回 400，未新增 code | 無（既有碼，僅新增觸發途徑） |
