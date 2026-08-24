銷售組織主檔規格書
===
---

## 概述

銷售組織是客戶與營業所的上層組織單位，作為系統中組織樹的最頂層。

---

## 資料結構

> 欄位定義（名稱、型別、nullable）以 Entity 為準，個別欄位語意見其 Javadoc：
> - `src/main/java/com/agony/wmsallocation/entity/master/SalesOrganization.java`

結構重點（不隨欄位改名變動的部分）：

- 主鍵為 `salesOrgCode`（字串代碼，非自增 ID）
- 狀態分 ACTIVE / INACTIVE，查詢時通常僅顯示啟用的銷售組織

---

## 關聯結構

```
SalesOrganization
├── Branch（營業所）
└── Customer（透過 salesOrgCode + userCode 歸屬）
```

---

## API 設計

> 端點定義（路徑、HTTP method、請求/回應格式）以 Controller 為準，或由 Swagger/OpenAPI 自動產生。

對外提供的能力：

- 查詢銷售組織清單
- 查詢單一銷售組織

---

## 相關規格書

- [營業所主檔規格書](./Branch.md)
- [客戶主檔規格書](./Customer.md)

---
