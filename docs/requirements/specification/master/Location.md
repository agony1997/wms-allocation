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

- **業務碼為 `locationCode`，全域唯一**（2026-08-26 定案）——不同營業所不得共用同一代碼。
  `branchCode` 一律由本主檔反查，不由呼叫端指定（例：`SalesReceiveOrderService.receive` 只收 locationCode）。
  這讓 `AllocationOrderDetail` 這類只帶 `locationCode`、不帶 `branchCode` 的單據明細沒有歧義。
  與其他主檔（Branch／Product／Customer／Factory／SalesOrganization）一致，皆為單欄 `@Column(unique = true)`
- 大庫（WAREHOUSE）的 `locationCode` 等於 `branchCode`，`userCode` 為 null
- 業務員儲位（CAR）的 `userCode` 指向負責業務員

---

## 儲位類型

| 類型 | 說明 | locationCode | userCode |
|------|------|--------------|--------|
| WAREHOUSE | 大庫 | = branchCode | null |
| CAR | 業務員車存 | 營業所碼 + 所內序號 | 業務員 userCode |

---

## 編號規則（2026-08-26 定案）

`locationCode` 為 **4 位數字**，切成 `營業所碼(2) + 所內序號(2)`：

| 區段 | 值 | 用途 |
|------|-----|------|
| 所內序號 `00` | `1000`、`1100`、`1200`… | **大庫**，每所恰有一個 |
| 所內序號 `01`–`99` | `1011`、`1012`…；`1110`、`1111`… | **車存**，所內編號，不要求連續 |

- `branchCode` 即該所的大庫代碼（沿用「大庫 `locationCode` = `branchCode`」）：營業所 10 → `1000`、營業所 11 → `1100`
- 容量上限：99 個營業所 × 每所 99 個車存儲位。超過須改為 5 位碼，屬編碼規則變更
- **此為資料層慣例，不做程式驗證**——儲位主檔目前唯讀（無新增／修改端點），沒有需要驗證的入口。
  日後開放維護端點時再評估是否加格式檢查

### 對照（`data.sql` 現況）

| locationCode | branchCode | locationType | userCode | 說明 |
|--------------|------------|--------------|--------|------|
| 1000 | 1000 | WAREHOUSE | null | 營業所 10（信義總部）的大庫 |
| 1011 | 1000 | CAR | U001 | 王小明的車存，營業所 10 的 11 號 |
| 1100 | 1100 | WAREHOUSE | null | 營業所 11（北屯）的大庫 |
| 1110 | 1100 | CAR | U002 | 李小華的車存，營業所 11 的 10 號 |
| 1200 | 1200 | WAREHOUSE | null | 營業所 12（霧峰，停用中）的大庫 |

---

## 關聯關係

```
Branch (1) ─────< Location (N)
User (1) ─────< Location (N)   // 一個業務員可有多儲位（不同營業所）
```

### 一人多儲位的範例

同一業務員在不同營業所各有一個車存儲位；因 `locationCode` 全域唯一，兩者是不同代碼：

| locationCode | branchCode | locationType | userCode | 說明 |
|--------------|------------|--------------|--------|------|
| 1011 | 1000 | CAR | U001 | U001 在營業所 10 的車存 |
| 1112 | 1100 | CAR | U001 | 同一個 U001 在營業所 11 的車存 |

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
