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
- **`AuthUser` 不掛營業所欄位，系統無「主要營業所」概念**（2026-08-28 定案）。
  歸屬有且只有兩個來源：角色歸屬看 `AuthUserBranchRole`、儲位歸屬看 `Location`
  （其自帶 `branchCode` 與 `userCode`）。兩者是各自獨立的事實——
  在某所有角色卻無儲位、或有儲位卻無角色，都是合法狀態

> **為何刪掉 `AuthUser.branchCode`**：該欄位曾存在（javadoc 稱「主要所屬營業所」，預設 `"9999"`），
> 但從未進入本規格，也無任何業務邏輯讀它——唯一的使用者是舊版 `AuthService.resolveRole()`
> 的「優先取主要營業所的角色」，該邏輯已隨多角色改造移除。
> 它與上述兩個來源之間無 FK、無一致性保證，留著只會製造矛盾的第三個真相。
>
> 連帶影響：**營業所刪除前的「人員」引用檢查須查 `AuthUserBranchRole`**，不可查 `AuthUser`——
> 後者會漏掉「在該所有角色」的人，刪除後留下孤兒角色關聯。

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
| 單據與庫存**讀取**（訂貨網格、SPO、BPO、FDO、配貨單、領貨單、庫存／異動／快照） | ✅ | ✅ | ✅ | ✅ |

> 2026-09-07 補列（原矩陣只寫了主檔讀取）：單據與庫存讀取比照主檔讀取，功能層對四個角色全開，
> 「看得到哪幾列」由「資料範圍授權」段的 Service 層檢查決定。依據有二：
> 該段 SALES 一列本來就寫著「及其所屬營業所的**唯讀資料**」；且主線每一段都要先讀才能做
> （配貨要看待配 SPOD、領貨要看待領明細、收貨要看待收清單），在功能層限縮讀取會直接擋掉主線。

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

### 三層分工（2026-08-28 定案）

| 層 | 擋什麼 | 依據 | 是否為安全機制 |
|----|--------|------|----------------|
| 前端路由／按鈕顯示 | 頁面看不看得到 | `LoginResponse` 的 `branchRoles` | ❌ **純 UX**，帶著 token 用 curl 可完全繞過 |
| 攔截器 `@RequireRole` | API 打不打得到 | token 的 `branchRoles` | ✅ |
| Service 層範圍檢查 | 這筆資料動不動得了 | token + 查主檔／單據 | ✅ |

第一層與第二層讀同一份資料卻**不等價**——前端藏起來的按鈕，直接打 API 一樣打得到。
第一層存在的唯一理由是不要讓使用者按下去才被拒絕。

### `@RequireRole` 採「任一營業所」語意（2026-08-28 定案）

`@RequireRole("LEADER")` 判定的是「此人**在任一營業所**具備 LEADER」，
**不**判定「在本次操作的營業所具備 LEADER」。後者一律由 Service 層負責。

標註值改為陣列、語意為 OR（`@RequireRole({"LEADER", "ADMIN"})`）——
權限矩陣每一列都是「某角色**或** ADMIN」，單值型別連寫都寫不出來。

**為何不在攔截器一次驗完**——盤點矩陣涉及的端點，本次操作的營業所只有一半讀得到：

| 功能 | 端點 | branch 來源 | 攔截器讀得到 |
|------|------|------------|-------------|
| 凍結／解除凍結／確認 BPF | `POST /api/branch-purchases/actions/*` | `@RequestParam branchCode` | ✅ |
| 調整 confirmedQty | `PUT /api/branch-purchases/adjust` | `@RequestParam branchCode` | ✅ |
| 彙總 BPO | `POST /api/branch-purchase-orders/actions/aggregate` | `@RequestParam branchCode` | ✅ |
| 配貨 AO | `POST /api/allocation-orders/actions/allocate` | `@RequestParam branchCode` | ✅ |
| 建立 SPO | `PUT /api/sales-purchase-orders` | body 的 `SavePurchaseRequest.branchCode` | ❌ 需讀 body |
| 收貨 FDO | `POST /api/factory-delivery-orders/actions/receive` | body 僅 `fdoNo`，須查單據 | ❌ |
| 領貨 SRO | `POST /api/sales-receive-orders/actions/receive` | 僅 `locationCode`，須查 Location 主檔 | ❌ |
| Mock 出貨 FDO | `POST /api/factory-delivery-orders/actions/ship` | 僅 `bpoNo`，須查 BPO | ❌（ADMIN only，無妨） |

理由三條：

1. **不均勻的保護比沒有更危險**。四支讀不到，其中三支是主線動作。一旦攔截器「有時候」做了
   營業所判定，後續開發者會假設它做完了——而該假設恰好在沒做的那幾支上是錯的。
2. **最需要它的地方它答不出來**。SALES 的範圍是「自己擁有的儲位」（見上表），不是營業所。
   領貨 SRO 就算反查出 branchCode、確認他在該所是 SALES，仍答不出「這個儲位是不是他的」——
   同所的另一位業務員完全滿足前者，卻不能領別人儲位的貨。
3. **錯誤品質**。攔截器擋 → 裸 403 無 body；Service 擋 → `BusinessException` + ErrorCode + 訊息。

**已接受的代價**：在任一營業所具備某角色的人，打得到所有該角色端點的 Controller 門口，
唯一防線是 Service 層有寫檢查。因此該檢查是**必要**而非加分項。

### 標註覆蓋率：每支端點都要標，未標視為漏掛（2026-09-07 定案）

Controller 的 58 支端點全部掛上 `@RequireRole`，包含純讀取端點——讀取標的是全部四個角色，
語意上等於「只要有 token」（無角色關聯的帳號不得登入，見下段）。

**為何不讓讀取端點留白**：`HandlerInterceptor` 預設 allow-all，漏掛不會有任何人提醒。
留白就分不出「刻意開放」與「忘記掛」；標滿之後「沒有標註」才是一個可以被測試釘死的訊號。
代價是四角色標註在語意上是 no-op、讀起來像雜訊，且日後新增角色要回頭改 32 處——
但那正是明示式的好處：新角色不會默默取得全部讀取權。

唯一例外是 `POST /api/auth/login`（在 `WebMvcConfig` 排除攔截，呼叫時還沒有身分可判定）。

### `/actuator/**` 不在攔截器覆蓋範圍內，以「不曝光」關閉（2026-09-07 定案）

`JwtInterceptor` 技術上蓋不到 actuator：`WebMvcConfigurer.addInterceptors` 註冊的攔截器只會被塞進
`WebMvcConfigurationSupport` 自己建的那幾個 handler mapping，而 actuator 另有一組
`WebMvcEndpointHandlerMapping`，它只會偵測 `MappedInterceptor` 型別的 **bean**
（Spring Boot 3.4.1 原始碼確認）。因此把 `addPathPatterns` 由 `/api/**` 改成 `/**`
**不會**保護到 actuator——那是「以為有保護、其實沒有」。

改法是收斂曝光面而非加保護：`management.endpoints.web.exposure.include` 只留 `health`
（保留給日後的 docker healthcheck）、`show-details=never`。原先 `include=health,info,metrics`
搭配 `show-details=always` 會把資料庫連線狀態吐給未登入者。

`show-details` 不採 `when-authorized`：該值靠 Spring Security 的 principal 判斷，本專案手刻 JWT
（[ADR-0010](../../../adr/0010-custom-jwt-auth-without-spring-security.md)）永遠取不到 principal，
行為等於 `never`，寫 `never` 才不會誤導後人以為「登入後看得到」。

> 未處理的部分：攔截器仍然只攔 `/api/**`，即 fail-open 的體質沒變——日後新增非 `/api/` 前綴的
> 路徑（例如 webhook）不會自動受保護。要根治得改用 `MappedInterceptor` bean 註冊、預設全擋、
> 例外才排除（並須排除 `/error`，否則未登入者的 404 會變成 401）。目前全部端點都在 `/api/` 底下，
> 不值得為此改變註冊方式。

### Service 層的兩個檢查入口

不要每支 Service 各寫各的 `if`——漏一支就是越權。收斂為兩個方法：

| 方法 | 用於 | 驗什麼 |
|------|------|--------|
| `assertBranchAccess(branchCode, 需要的角色)` | LEADER／WAREHOUSE 的營業所級操作 | ① `branchCode` ∈ token `branchRoles` 的 keys；② **且該 key 底下有需要的角色** |
| `assertLocationOwnership(locationCode)` | SALES 的儲位級操作（訂貨、領貨） | `Location.userCode` = 當前登入者 |

第 ② 點不可省。只驗 ① 的話，「在 1000 是 SALES、在 1100 是 WAREHOUSE」的人送
`branchCode=1000` 呼叫配貨會通過——他確實「有」1000 的權限，只是不是配貨的權限。

**ADMIN 一律放行，兩個方法都不比對**——依上表其資料範圍為「不限」。
這是全系統唯一的角色特例，前端選單也須對應（見「前端營業所選擇器」段）。

每支需要範圍檢查的 Service 方法，都要有一支「別所／別人的儲位打進來要被擋」的測試，
以及一支「ADMIN 打進來要放行」的測試。

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

## 登入契約與前端營業所選擇（2026-08-28 定案）

### 無角色關聯的使用者不得登入

`AuthUserBranchRole` 一筆都沒有的帳號，`AuthService.login` 直接拒絕，
拋 `AUTH_NO_ROLE_ASSIGNED`（403）。

**為何不放行**：放行的話他拿得到 token，但空的 `branchRoles` 讓每支 `@RequireRole` 端點都回
403、前端營業所選單是空的——登入後無事可做的死路。在登入處一次講清楚，比讓他在五個頁面
各撞一次 403 誠實。

**為何不給預設角色**：那是在程式裡發權限，`AuthUserBranchRole` 查不到這筆授權，
日後稽核「誰有什麼權限」時對不起來。權限只能有資料庫這一個來源。

### `LoginResponse` 契約

```
{ token, userCode, userName, branchRoles }
```

- `branchRoles`：`Map<branchCode, List<roleCode>>`，與 token claim 同一份資料
- 原本的單值 `role` 欄位移除——一人多角時它必然是謊話
- **沒有** `defaultBranchCode` 或任何單值營業所欄位。使用者的營業所歸屬只有
  `branchRoles` 這一個來源，見「資料結構」段：系統無「主要營業所」概念

### 前端營業所選擇器

多營業所人員需在頁面上指定「本次操作哪個營業所」，該值即各端點的 `branchCode` 參數。

**採每頁各自的營業所欄位**（2026-08-28 定案），不做全域「當前作業營業所」。

1. **選單列「在該所具備本頁所需角色」的營業所**；ADMIN 列**全部營業所**（見下）。
   正常資料下這個過濾是 no-op——一個人跨營業所通常擔任同一職務（見「範例：多營業所角色」：
   U001 跨所都是業務線、U002 跨所都是庫務），因此**各頁看到的清單一致**。
   過濾是防禦性的：真出現「甲所業務、乙所庫務」這種人時，不讓他看到選了必然 403 的選項。
   成本為零——頁面本來就要宣告自己需要什麼角色（決定要不要出現在導覽列），順手複用
2. **欄位一律顯示在畫面上並自動帶入**，不留空——使用者隨時看得見自己在對哪個所操作，
   也省掉「請先選擇營業所」這個驗證狀態
3. **預設值取「本頁合法集合」中排序最小者**，合法集合只有一個時欄位鎖定唯讀。
   多營業所使用者拿到的是固定但無業務意義的值，這是刻意的：系統無「主要營業所」概念
   （見「資料結構」段），沒有更好的依據可挑，穩定可預期即足夠。
   **注意是本頁合法集合的最小值，不是 `branchRoles` 全部 keys 的最小值**——
   取錯會帶入一個本頁不合法的所
4. **選擇器決定「送什麼」，不決定「能做什麼」**——它讓 `branchCode` 成為完全可控的輸入
   （改 devtools、直接打 API），後端一律不信任，見「資料範圍授權」段

#### ADMIN 的選單列出全部營業所（2026-08-28 定案）

依「資料範圍授權」表，ADMIN 的資料範圍是「不限」，因此其選單不受 `branchRoles` 的 keys 限制，
需另行取得營業所清單（`GET /api/branches`）。

後端須一致：`assertBranchAccess` 對 ADMIN 一律放行，不比對 `branchRoles`。
**前後端必須同時實作**——只做前端會讓 ADMIN 選了別的所卻被後端擋，只做後端則他選不到。

#### 為何不做全域「當前作業營業所」

主要理由是**欄位就在畫面上**：使用者隨時看得見自己在對哪個所操作。
全域切換器在 header 角落容易被看漏，導致對錯的營業所下單，而這種錯誤事後極難察覺。

次要理由是全域值可能在某些頁面不合法（一人跨所擔任不同職務時），需要額外一套處理。
但如規則 1 所述，正常資料下這種情況不出現，故此理由權重不高。

> **一併刻意不做**「記住上次用過的營業所」。它能省下多營業所使用者的跨頁重選，
> 代價是引入前端持久化狀態，以及「記住的值在本頁不合法」的 fallback。
> 跨所人員是少數，不值得為此增加狀態。日後真的嫌煩再加，加的位置就是規則 3。

---

## API 設計

> 端點定義（路徑、HTTP method、請求/回應格式）以 Controller 為準，或由 Swagger/OpenAPI 自動產生。

對外提供的能力：

- 查詢使用者清單
- 查詢單一使用者
- 查詢營業所下的使用者——須經 `AuthUserBranchRole`（角色歸屬）或 `Location`（儲位歸屬）反查，
  兩者語意不同，端點須表明是哪一種；`AuthUser` 本身不帶營業所
- 查詢角色清單
- 查詢使用者的角色

---

## 相關規格書

- [營業所主檔規格書](./Branch.md)
- [儲位主檔規格書](./Location.md)

---
