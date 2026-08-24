儲位主檔規格書
===
---

## 概述

儲位代表庫存存放位置，分為大庫（WAREHOUSE）和業務員車存（CAR）。
每個營業所都有一個大庫，大庫的 locationCode = branchCode。

---

## 資料結構

> 欄位定義（名稱、型別、nullable）以 Entity 為準，個別欄位語意見其 Javadoc：
> - `src/main/java/com/agony/wmsallocation/entity/branch/Location.java`

結構重點（不隨欄位改名變動的部分）：

- 主鍵為 `locationCode`（字串代碼）
- 大庫（WAREHOUSE）的 `locationCode` 等於 `branchCode`，`userCode` 為 null
- 業務員儲位（CAR）的 `userCode` 指向負責業務員

---

## 儲位類型

| 類型 | 說明 | locationCode | userCode |
|------|------|--------------|--------|
| WAREHOUSE | 大庫 | = branchCode | null |
| CAR | 業務員車存 | 業務員儲位代碼 | 業務員 userCode |

---

## 關聯關係

```
Branch (1) ─────< Location (N)
User (1) ─────< Location (N)   // 一個業務員可有多儲位（不同營業所）
```

### 範例

| locationCode | branchCode | locationType | userCode | 說明 |
|--------------|------------|--------------|--------|------|
| 1000 | 1000 | WAREHOUSE | null | 營業所 1000 的大庫 |
| S001 | 1000 | CAR | U001 | 業務員 U001 在營業所 1000 的車存 |
| S002 | 1000 | CAR | U002 | 業務員 U002 在營業所 1000 的車存 |
| S003 | 2000 | CAR | U001 | 業務員 U001 在營業所 2000 的車存 |

---

## 庫存關聯

```
Location
└── Inventory (庫存)
    ├── qty: 可用數量
    ├── keepQty: 寄庫數量 (僅 WAREHOUSE)
    └── returnQty: 待退庫數量 (僅 WAREHOUSE)
```

---

## API 設計

> 端點定義（路徑、HTTP method、請求/回應格式）以 Controller 為準，或由 Swagger/OpenAPI 自動產生。

對外提供的能力：

- 查詢儲位清單
- 查詢單一儲位
- 查詢營業所下的儲位
- 查詢業務員的儲位

---

## 相關規格書

- [營業所主檔規格書](./Branch.md)
- [使用者規格書](./User.md)
- [庫存規格書](./Inventory.md)

---
