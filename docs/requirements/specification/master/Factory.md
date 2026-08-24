工廠主檔規格書
===
---

## 概述

工廠基本資訊，用於產品對應、訂貨、銷退等流程。

---

## 資料結構

> 欄位定義（名稱、型別、nullable）以 Entity 為準，個別欄位語意見其 Javadoc：
> - `src/main/java/com/agony/wmsallocation/entity/master/Factory.java`

結構重點（不隨欄位改名變動的部分）：

- 主鍵為 `factoryCode`（字串代碼）
- BPO、FDO、BRO 均以工廠為單位分組產生

---

## 單據關係

```
Factory
├── ProductFactory (產品對應)
├── BPO (營業所訂貨單，按工廠分)
├── FDO (工廠出貨單)
└── BRO (營業所銷退單，按工廠分)
```

---

## API 設計

> 端點定義（路徑、HTTP method、請求/回應格式）以 Controller 為準，或由 Swagger/OpenAPI 自動產生。

對外提供的能力：

- 查詢工廠清單
- 查詢單一工廠

---

## 相關規格書

- [產品主檔規格書](./Product.md)

---
