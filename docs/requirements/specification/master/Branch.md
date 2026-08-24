營業所主檔規格書
===
---

## 概述

營業所基本資訊，是系統中的主要組織單位。

---

## 資料結構

> 欄位定義（名稱、型別、nullable）以 Entity 為準，個別欄位語意見其 Javadoc：
> - `src/main/java/com/agony/wmsallocation/entity/branch/Branch.java`

結構重點（不隨欄位改名變動的部分）：

- 主鍵為 `branchCode`（字串代碼，非自增 ID）
- 狀態分 ACTIVE / INACTIVE，查詢時通常僅顯示啟用的營業所

---

## 關聯結構

```
Branch
├── Location (儲位，含大庫和業務員車存)
├── User (所屬人員)
├── Customer (負責客戶)
├── Inventory (庫存)
└── 各類單據 (SPO, BPO, FDO, AO, SDO, SKR, SRR, BRO...)
```

---

## API 設計

> 端點定義（路徑、HTTP method、請求/回應格式）以 Controller 為準，或由 Swagger/OpenAPI 自動產生。

對外提供的能力：

- 查詢營業所清單
- 查詢單一營業所

---

## 相關規格書

- [儲位主檔規格書](./Location.md)
- [使用者規格書](./User.md)

---
