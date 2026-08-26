使用者規格書
===
---

## 概述

系統使用者，包含業務員、組長、庫務、系統管理員等角色。

---

## 資料結構

> 欄位定義（名稱、型別、nullable）以 Entity 為準，個別欄位語意見其 Javadoc：
> - `src/main/java/com/agony/wmsallocation/entity/auth/AuthUser.java`
> - `src/main/java/com/agony/wmsallocation/entity/auth/AuthRole.java`
> - `src/main/java/com/agony/wmsallocation/entity/auth/AuthUserBranchRole.java`

結構重點（不隨欄位改名變動的部分）：

- 主鍵為 `userCode`（員工編號，字串）
- 角色透過 `AuthUserBranchRole` 三向關聯（使用者 × 營業所 × 角色），唯一鍵為 (userCode, branchCode, roleCode)
- 一個業務員可在多個營業所扮演不同角色，並持有對應儲位

---

## 角色定義

| 角色代碼 | 名稱 | 權限說明 |
|----------|------|----------|
| SALES | 業務員 | 訂貨、領貨、送貨、寄庫、退庫 |
| LEADER | 組長 | 凍結、調整確認數量、業務員管理 |
| WAREHOUSE | 庫務 | 收貨、配貨、彙整銷退 |
| ADMIN | 系統管理員 | 全部權限 |

---

## 關聯關係

```
AuthUser (1) ─────< AuthUserBranchRole (N) >───── AuthRole (1)
                           │
                           └───── Branch (1)

AuthUser (1) ─────< Location (N)   // 一個業務員可有多儲位
```

### 範例：多營業所角色

| userCode | branchCode | roleCode | 說明 |
|--------|------------|----------|------|
| U001 | 1000 | LEADER | U001 在營業所 1000 是組長 |
| U001 | 1000 | SALES | U001 在營業所 1000 也是業務員 |
| U001 | 2000 | SALES | U001 在營業所 2000 是業務員 |
| U002 | 1000 | WAREHOUSE | U002 在營業所 1000 是庫務 |
| U002 | 2000 | WAREHOUSE | U002 在營業所 2000 也是庫務 |

### 業務員與儲位

一個業務員可以負責多個儲位（可能在不同營業所）：

```
User U001
├── 營業所 1000
│   ├── 角色: LEADER + SALES
│   └── 儲位: S001
│
└── 營業所 2000
    ├── 角色: SALES
    └── 儲位: S003
```

---

## 權限矩陣

**功能授權**——誰可以呼叫哪支端點。由 `@RequireRole` 在攔截器層執行，不符回 403。

| 功能 | SALES | LEADER | WAREHOUSE | ADMIN |
|------|-------|--------|-----------|-------|
| 建立 SPO | ✅ | ✅ | - | ✅ |
| 凍結 BPF | - | ✅ | - | ✅ |
| 解除凍結 BPF | - | ✅ | - | ✅ |
| 確認 BPF | - | ✅ | - | ✅ |
| 調整 confirmedQty | - | ✅ | - | ✅ |
| 彙總 BPO | - | ✅ | ✅ | ✅ |
| 收貨 FDO | - | - | ✅ | ✅ |
| 配貨 AO | - | - | ✅ | ✅ |
| 領貨 SRO | ✅ | - | - | ✅ |
| 送貨 SDO | ✅ | - | - | ✅ |
| 寄庫 SKR | ✅ | - | - | ✅ |
| 退庫 SRR | ✅ | - | - | ✅ |
| 彙整 BRO | - | - | ✅ | ✅ |
| Mock 工廠出貨 FDO | - | - | - | ✅ |
| 手動觸發庫存快照 | - | - | - | ✅ |
| 主檔**寫入**（商品／客戶／工廠／銷售組織／營業所） | - | - | - | ✅ |
| 主檔**讀取** | ✅ | ✅ | ✅ | ✅ |

> 2026-08-26 補列（原矩陣未涵蓋）：解除凍結／確認 BPF 比照凍結歸 LEADER；
> 彙總 BPO 依 [BranchPurchase.md](../purchase/BranchPurchase.md)「主要操作者：庫務，組長」；
> Mock 工廠出貨依 [FactoryDeliveryOrder.md](../receive/FactoryDeliveryOrder.md)「系統管理員：Mock 模擬操作」；
> 手動快照為維運操作，歸 ADMIN；主檔讀取開放全部登入者（業務員訂貨要選商品、庫務配貨要看品名）。

---

## 資料範圍授權（2026-08-26 定案）

功能授權只回答「能不能做這件事」，不回答「能對誰的資料做」。兩者都要擋。

> **原則：不信任前端。** 呼叫端送來的 `branchCode` / `locationCode` 一律視為**不可信輸入**，
> 必須與 token 身分比對，不符即拒絕。規格中「業務員預設值：所屬營業所／所擁有儲位」那類敘述
> 指的是**前端下拉選單的預設值**，不是後端的邊界——前端可被繞過，後端才是防線。

| 角色 | 可存取的資料範圍 |
|------|------------------|
| SALES | 僅**自己擁有的儲位**（`Location.userCode` = 當前登入者），及其所屬營業所的唯讀資料 |
| LEADER / WAREHOUSE | 其**有權限的營業所**（`AuthUserBranchRole`）底下的全部儲位與單據 |
| ADMIN | 不限 |

- LEADER 可代業務員操作該所任一儲位——依 [SalesPurchase.md](../purchase/SalesPurchase.md)「營業員的上司有權限可代為訂貨」
- 無範圍參數的查詢端點（如列出全部庫存、全部使用者）須**依身分過濾**，不可直接回全公司資料
- 檢查落在 **Service 層**（Controller 只有參數、沒有資料，判斷儲位歸屬需查主檔）；
  功能授權留在攔截器的 `@RequireRole`，兩層分工

---

## Token 攜帶角色與撤銷時效（2026-08-26 定案）

### 決策：角色與營業所歸屬放進 token

```
claim("branchRoles", {"1000": ["SALES", "LEADER"], "1100": ["WAREHOUSE"]})
```

`UserContextHolder` 由兩個字串升級為 `userCode` + 一份 `Map<branchCode, Set<role>>`，
供 `@RequireRole`（功能授權）與 Service 層（資料範圍授權）共用同一份來源。

**為何不每請求查 DB**：角色與營業所歸屬是 HR 層級資料，一人一年變動數次且皆為行政動作。
用每一個請求的查詢成本，換一年數次的即時性，划不來。token 大小不是問題
（10 個營業所 × 2 角色僅數百 bytes）；payload 可被持有者讀取也不是問題（內容只是他自己的權限）。

**已知缺口**：token 一經簽發即不可撤回，故角色異動的生效延遲 =
token 生命週期（目前 **8 小時**）。授予延遲無害（重新登入即可）；
**撤銷延遲**才是風險——被拔權的人最長可續用舊權限 8 小時。

同一個缺口也體現在帳號停用上：`AuthService.login` 會擋 `status != ACTIVE`，
但**只擋新登入，不影響已發出的 token**。

**現階段不處理**，理由：系統目前**沒有任何角色管理端點**（`/api/users` 僅兩支 GET，
無 Role 相關 Controller／Service），角色只能改 DB，「拔角色的那一刻」在程式中不存在，
撤銷機制無處掛載。

### 升級階梯

| 需求出現時 | 做法 | 撤銷窗口 |
|-----------|------|---------|
| 現況 | 不處理 | 8 小時 |
| 想縮短窗口 | 短 access token（15 分）+ refresh token | 分鐘級 |
| 需要**立即**停權 | 再加撤銷清單 | 即時 |

第二階的關鍵：refresh 把「讀角色」的頻率由每請求降為每 15 分鐘（換發 access token 時才讀 DB），
而非降為零。順帶解掉 8 小時 session 無續期、到點被硬踢的 UX 問題。

### 若要實作立即停權

前提是先有角色管理／帳號停用端點，撤銷才有觸發點。要點如下：

1. **以「撤銷時間戳」記錄，不用布林旗標**——存 `userCode → revokedAt`。
   驗證時比對 token 的 `iat`：`iat < revokedAt` 即視為失效。
   用布林會把「停權後重新啟用、重新登入拿到的新 token」一併擋掉。
2. **粒度取 `userCode`**，不取 `jti`。停權針對的是人，且同一人可能有多個裝置的 token。
3. **存放**：單副本用 `ConcurrentHashMap` 即可；多副本必須外移至 Redis，
   否則 A 副本撤銷、B 副本不知情。重啟後記憶體版清單會消失，而彼時舊 token 可能尚未過期。
4. **TTL 設為 token 最長壽命**（8 小時）。超過該時間的舊 token 本已自然失效，紀錄可自動清除，
   清單因此不會無限成長。
5. **檢查點**：`JwtInterceptor.preHandle` 驗簽通過之後、寫入 `UserContextHolder` 之前。
6. **成本認知**：撤銷清單並非零成本——它把「每請求檢查」的對象由 DB 換成記憶體／Redis，
   **檢查頻率不變**。這是選它之前必須認清的取捨。

---

## API 設計

> 端點定義（路徑、HTTP method、請求/回應格式）以 Controller 為準，或由 Swagger/OpenAPI 自動產生。

對外提供的能力：

- 查詢使用者清單
- 查詢單一使用者
- 查詢營業所下的使用者
- 查詢角色清單
- 查詢使用者的角色

---

## 相關規格書

- [營業所主檔規格書](./Branch.md)
- [儲位主檔規格書](./Location.md)

---
