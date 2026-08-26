業務領貨單規格書 (SRO)
===
> 主要操作者：業務員
---

## 概述

業務員上班時，根據已配貨的明細 (AOD) 進行點貨確認，領取貨品至自己的儲位。
一次領取可包含來自多張配貨單 (AO) 的明細。

---

## 作業流程

```
┌─────────────────────────────────────────────────────────────────────────┐
│  階段 1: 查詢待領貨                                                      │
│  ───────────────                                                        │
│  - 業務員查詢自己的待領明細 (AOD.status = PENDING)                        │
│  - 可能來自多張 AO（不同配貨時間）                                        │
└─────────────────────────────────────────────────────────────────────────┘
        │
        ▼ 業務員點貨確認
┌─────────────────────────────────────────────────────────────────────────┐
│  階段 2: 確認領取                                                        │
│  ─────────────                                                          │
│  1. 業務員點貨，確認品項、批次、數量                                      │
│  2. 系統產生 SRO + SROD                                                  │
│  3. SROD 關聯來源 AOD                                                    │
│  4. 更新 AOD.status = RECEIVED                                          │
│  5. 庫存從大庫移轉至業務員儲位                                            │
└─────────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  階段 3: 完成                                                            │
│  ─────────                                                              │
│  - 業務員儲位庫存增加                                                    │
│  - 可進行送貨作業                                                        │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 資料結構

> 欄位定義（名稱、型別、nullable）以 Entity 為準，個別欄位語意見其 Javadoc：
> - 單頭：`src/main/java/com/agony/wmsallocation/entity/allocation/SalesReceiveOrder.java`
> - 明細：`src/main/java/com/agony/wmsallocation/entity/allocation/SalesReceiveOrderDetail.java`

結構重點（不隨欄位改名變動的部分）：

- SRO 單頭記錄領貨的**營業所**、**業務員儲位 (CAR)**、領貨日期（領取時間由 `AuditEntity.createdAt` 記錄，不另設欄）
- SROD 明細逐項指向**來源 AOD（配貨單明細）**，記錄產品、批次、效期、**領取數量**

> **設計決定：不設「領取人員 (receivedBy)」欄位。** 領貨人即儲位擁有者（`locationCode` → `Location.userCode`），可推導、不另存；
> 即使有代理領貨/代送，「誰來領」並非業務重點，遺失追溯時操作者也已由 `AuditEntity.createdBy` 記錄。故不為可推導且非業務焦點的資料另開欄位。
> （原欄位表的命名差異——如 `aodItemNo` → `allocationItemNo`——改以 Entity 為準後即不再需要追蹤）

---

## SRO 狀態（2026-07-09 定案：無狀態欄）

SRO 於業務員**點貨確認當下**建立，一出生即為已領取——單值狀態不需要狀態欄：

- Entity 的 `status` 欄位移除（`AllocationStatus` 僅供 AOD 使用）；`receivedAt` 與 `AuditEntity.createdAt` 重複，建議一併移除
- 「待領清單」**不是預產的單據**，而是查詢時即時合成：AOD（status = PENDING）＋昨日寄庫 SKR（status = KEPT，closing 階段擴充）
- 不預產 SRO、不使用排程

---

## 領貨範圍（2026-08-26 定案：一次全領）

原規格未寫明「業務員能否只領一部分」，此處定案為**該儲位所有待領明細一次領完**：

- 確認領貨的請求只帶 `branchCode` + `locationCode`，**不帶明細清單**——沒有勾選、沒有部分領取
- 因此「今天車裝不下，剩的明天領」目前做不到。要支援時須改為請求帶 AOD 清單，屬 API 契約變更
- 無待領明細時**冪等返回**：不取號、不建單、不回錯（比照 `AllocationService.allocate`）

> 與「AOD : SROD = 1 : 1（一個 AOD 一次領完）」不衝突：那條講的是**單筆 AOD 不可分次領**，
> 本節講的是**所有 AOD 必須一起領**。兩者是不同層級的限制。

### 待領明細的查詢範圍

`AllocationOrderDetail` 沒有 `branchCode`，但 `locationCode` 全域唯一
（見 [Location 規格](../master/Location.md)），故單鍵查詢即無歧義：
`AOD.locationCode = ? AND status = PENDING`，不需要先由 branchCode 收斂。

領貨的 `branchCode`（供庫存異動用）由儲位主檔反查取得，**不由呼叫端指定**——
呼叫端無從把貨領進別的營業所。

---

## 單據關係

```
配貨 1: AO-1 ─┬─ AOD (F1 的貨) ──┐
              ├─ AOD (F2 的貨) ──┼─ 待領清單 ─┐
              └─ AOD (H1 的貨) ──┘            │
                                             ├─► 業務員 S 領貨 ─► SRO-1
配貨 2: AO-2 ─── AOD (H2 的貨) ─── 待領清單 ─┘            │
                                                         ▼
                                                  SRO-1 的 SROD
                                                  ├── 關聯 AO-1 的 AOD
                                                  └── 關聯 AO-2 的 AOD
```

### 關聯方式

- AOD : SROD = 1 : 1（一個 AOD 一次領完）
- AO : SRO = N : 1（一張 SRO 可包含多張 AO 的明細）

> **SROD 來源的分階段設計（2026-07-09 定案）**：allocation 階段 SROD 僅來自 AOD（現行 `allocationNo` / `allocationItemNo` 為 NOT NULL）；closing 階段實作寄庫領回時，再放寬為 nullable 並加 `sourceType`（ALLOCATION / KEEP_RETRIEVE）＋ `keepNo` / `keepItemNo`（擇一非空由 Service 驗證），屆時本節「1:1 AOD」限定同步放寬。與[業務員寄庫單規格書](../closing/SalesKeepRecord.md)的「自動併入」不衝突，是分階段實作。

---

## API 設計

> 端點定義（路徑、HTTP method、請求/回應格式）以 Controller 為準，或由 Swagger/OpenAPI 自動產生。

對外提供的能力（操作者：業務員）：

- 查詢待領貨明細
- 確認領貨
- 查詢領貨單清單
- 查詢單一領貨單明細

---

## 庫存影響

### 領貨確認後

| 動作 | 說明 |
|------|------|
| 大庫庫存 | 已在配貨時扣減（預留） |
| 業務員儲位庫存 | 增加（含批次、效期） |

---

## 相關規格書

- [配貨單規格書](./AllocationOrder.md)
- [業務員訂貨規格書](../purchase/SalesPurchase.md)

---
