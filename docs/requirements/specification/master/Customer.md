客戶主檔規格書
===
---

## 概述

客戶基本資訊，用於預訂、送貨、應收帳款等流程。

---

## 資料結構

> 欄位定義（名稱、型別、nullable）以 Entity 為準，個別欄位語意見其 Javadoc：
> - `src/main/java/com/agony/wmsallocation/entity/master/Customer.java`

結構重點（不隨欄位改名變動的部分）：

- 主鍵為 `customerCode`（字串代碼）
- 客戶歸屬於某營業所，並對應一個負責業務員儲位（`locationCode`）

---

## 單據關係

```
Customer
├── CPO (客戶預訂單)
├── SDO (送貨單)
└── AR (應收帳款)
```

---

## API 設計

> 端點定義（路徑、HTTP method、請求/回應格式）以 Controller 為準，或由 Swagger/OpenAPI 自動產生。

對外提供的能力：

- 查詢客戶清單
- 查詢單一客戶
- 查詢業務員負責的客戶

---

## 相關規格書

- [客戶預訂單規格書](../delivery/CustomerPreOrder.md)
- [送貨單規格書](../delivery/SalesDeliveryOrder.md)

---
