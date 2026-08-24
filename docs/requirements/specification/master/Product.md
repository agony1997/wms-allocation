產品主檔規格書
===
---

## 概述

產品主檔及相關資料表，包含產品基本資訊、工廠對應、單位轉換。

---

## 資料結構

> 欄位定義（名稱、型別、nullable）以 Entity 為準，個別欄位語意見其 Javadoc：
> - `src/main/java/com/agony/wmsallocation/entity/master/Product.java`
> - `src/main/java/com/agony/wmsallocation/entity/master/ProductFactory.java`
> - `src/main/java/com/agony/wmsallocation/entity/master/ProductUnitConversion.java`

結構重點（不隨欄位改名變動的部分）：

- 主鍵為 `Integer id`（自動遞增），`productCode` 為唯一業務代碼
- 一個產品可對應多個工廠（`ProductFactory`），並標記預設工廠
- 單位轉換以獨立表（`ProductUnitConversion`）管理，唯一鍵為 (productCode, fromUnit, toUnit)

### 刪除與啟用/停用設計

- 啟用狀態以 `status`（`ActiveStatus`：`ACTIVE` / `INACTIVE`）表示，與 Factory／Customer／Branch／SalesOrganization 等主檔一致
- 建立時一律預設 `ACTIVE`，前端不可指定；狀態切換（啟用/停用）屬獨立操作，目前所有主檔皆尚未提供對應 API
- `DELETE` 為硬刪除，不是軟刪除
- 刪除前是否仍被 `ProductFactory`／`ProductUnitConversion`／`BranchProductList` 引用的下轄檢查，待這些表補上 Repository 後再實作（程式碼中以 `ponytail` 註記標示）

---

### ProductFactory（產品工廠對應）

產品可能來自多個工廠，採用獨立對應表。

```
Product ──< ProductFactory >── Factory
```

---

### ProductUnitConversion（產品單位轉換）

#### 範例

| productCode | fromUnit | toUnit | conversionRate |
|-------------|----------|--------|----------------|
| P001 | 箱 | 個 | 12 |
| P001 | 個 | 箱 | 0.0833 |

---

## API 設計

| 方法 | 端點 | 說明 |
|------|------|------|
| GET | /api/products | 查詢商品清單（`activeOnly=true` 只回啟用中） |
| GET | /api/products/{productCode} | 查詢單一商品 |
| POST | /api/products | 新增商品 |
| PUT | /api/products/{productCode} | 更新商品（不可改 `productCode`／`status`） |
| DELETE | /api/products/{productCode} | 刪除商品（硬刪除，見上方刪除設計） |

`ProductFactory`（工廠對應）與 `ProductUnitConversion`（單位換算）目前僅有 Entity，尚未提供對應的 Repository／Service／Controller，故無查詢或維護 API。

---

## 相關規格書

- [工廠主檔規格書](./Factory.md)
- [庫存規格書](./Inventory.md)

---
