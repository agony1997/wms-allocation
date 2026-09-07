# TODO

> **暫時文件，全部做完就刪掉。**給自己看的，不是專案文件——正式的決策紀錄在 `docs/adr/`、
> 規格在 `docs/requirements/specification/`、進度在 `README.md` 的「系統範疇」表。
>
> 隔很久回來時：從「現在在哪」讀起，再跳到「下一步」。
> 要接續工作，跟 Claude 說：**「讀 TODO.md，我要做（某一項）」**。

---

## 現在在哪

主線 **訂貨 → 配貨 → 領貨** 三段後端全部完成。授權四步中的**第 1、2、3 步已完成**
（多角色改造 + 移除 `AuthUser.branchCode` + 58 支端點掛上 `@RequireRole`），測試 196 綠。

```
ea4ef99 feat: 支援一人多營業所多角色，移除主要營業所欄位
259f856 docs: 新增 TODO.md 追蹤授權這條線的待辦
dd8502e docs: 補齊權限矩陣、定案資料範圍授權與 token 角色時效
```

目前在**授權**這條線上，分四步，做完才輪到前端。**下一步是第 4 步「資料級授權」**——
那才是把 IDOR 關掉的一步，第 3 步只擋工種、不擋「動誰的資料」。

**第 3 步留了一件事給你自己做**：反射守門測試（見下方「✅ 已完成：掛 `@RequireRole` 到端點」
的「待你寫的守門測試」段），對照表已經列好。在它寫出來之前，58 支標註沒有任何自動驗證。

> 跑測試：`./mvnw.cmd test`
> `BranchRepoTest` 需要 Docker（Testcontainers）。Docker 沒開時它會 error，其餘 196 支照跑。

---

## ✅ 已完成：多角色改造（2026-08-28）

下方保留當時的問題描述與決策，是為了日後回頭看「為什麼長這樣」；
**要改動這一塊前先讀完，其中好幾條是踩過坑才定下來的**。

實際落地的檔案：`JwtUtil`（簽發／解析 `branchRoles`）、`UserContextHolder`（深層不可變 map +
`hasRole`）、`RequireRole`（值改陣列、OR 語意）、`JwtInterceptor`（任一營業所比對）、
`AuthService`（`resolveBranchRoles` + 無角色擋登入）、`LoginResponse`（四欄）、
`ErrorCode`＋`error-codes.md`（`AUTH_NO_ROLE_ASSIGNED`）、`AuthServiceTest`（7 支）、
移除 `AuthUser.branchCode` 連帶六處、前端 `stores/auth.js`＋`HomeView.vue`。

### 問題

`User.md` 的範例明寫同一人可在同一營業所同時是 LEADER 和 SALES，
權限矩陣裡「建立 SPO」SALES/LEADER 都能做、「凍結」只有 LEADER 能做，
所以 U001 需要兩個角色**同時生效**。但現在三處都是單角色：

| 檔案 | 現況 |
|------|------|
| `security/JwtUtil.java` | `.claim("role", role)` 單一字串 |
| `service/AuthService.java` | `resolveRole()` 從多筆「挑一個」（已有 ponytail 註記自承） |
| `security/UserContextHolder.java` | 兩個獨立的 `ThreadLocal<String>` |
| `security/JwtInterceptor.java` | `neededRole.equals(role)` 單值精確比對 |

### 已定的決策（別重新想）

- **token 塞 `branchRoles`**：`claim("branchRoles", {"1000":["SALES","LEADER"], "1100":["WAREHOUSE"]})`
- **不每請求查 DB**：角色歸屬是 HR 層級資料，一年變動數次，不值得用每請求成本換即時性
- **撤銷延遲 8 小時是已接受的缺口**，現在不做 refresh、不做撤銷清單
- **`resolveRole` 回傳 `Map<String, Set<String>>`，不做 DTO**（2026-08-28）——
  claim 與 `UserContextHolder` 兩端都是 map，中間插一層 `List<UserBranchRolesDto>`
  會變成三次形狀轉換；且它從未離開後端，不符合 DTO「跨層邊界」的定位。
  方法一併改名 `resolveBranchRoles`（回傳多筆卻叫單數會誤導）
- **`@RequireRole` 採「任一營業所」語意**（2026-08-28），營業所判定全部落在 Service 層
- **無角色關聯的帳號不得登入**（2026-08-28），新增 `AUTH_NO_ROLE_ASSIGNED`(403)
- **`LoginResponse` 移除單值 `role`**，改為 `branchRoles`；**不加** `defaultBranchCode`
  或任何單值營業所欄位（2026-08-28）
- **系統無「主要營業所」概念**，`AuthUser.branchCode` 移除（2026-08-28）——見下方獨立區塊
- 完整理由見 `docs/requirements/specification/master/User.md` 的
  「資料結構」「資料範圍授權」「Token 攜帶角色與撤銷時效」「登入契約與前端營業所選擇」四段

### 當時要做什麼（六項全部完成）

1. `JwtUtil` 簽發／解析 `branchRoles`
2. `AuthService.resolveRole()` → `resolveBranchRoles()`，回傳全部角色關聯（不再挑一個）；
   結果為空即拋 `AUTH_NO_ROLE_ASSIGNED`
3. 新增 `AUTH_NO_ROLE_ASSIGNED`(403) 到 `ErrorCode`，**同一 commit 內**同步
   `docs/api/error-codes.md`（鐵律 2）
4. `UserContextHolder` → `userCode` + `Map<branchCode, Set<String>>`，
   並提供 `hasRole(branchCode, role)` 查詢方法
5. `JwtInterceptor` 改為多角色比對（`@RequireRole` 值改陣列、語意 OR）
6. `LoginResponse` 改為 `{token, userCode, userName, branchRoles}`（四欄，**無** `defaultBranchCode`）；
   前端 `stores/auth.js`（三處）＋ `HomeView.vue`（一行）跟著改

### 同一批：移除 `AuthUser.branchCode`（2026-08-28 完成）

原本的想法從來沒有「主要營業所」，個人的營業所歸屬就是「在哪些所有角色／有儲位」——
`AuthUserBranchRole` 與 `Location`（自帶 `branchCode` + `userCode`）已完整表達，
`AuthUser.branchCode` 是牴觸該模型的殘留欄位，且與那兩張表之間無 FK。
完整理由見 `User.md`「資料結構」段。

**`data.sql` 已改（四筆 `auth_user` INSERT 移除該欄）**，所以下列改動必須**與它同一個 commit**——
欄位是 `nullable = false` 且無 DB 預設值，只改一邊會讓應用程式啟動時 INSERT 失敗。

| # | 改動 | 備註 |
|---|------|------|
| 1 | `AuthUser` 刪 `branchCode` 欄位 | 連同 javadoc |
| 2 | `AuthUserRepo.existsByBranchCode` 刪，改在 `AuthUserBranchRoleRepo` 新增同名方法 | **你寫**——第一次跨到關聯表查引用 |
| 3 | `BranchService.java:86` 的「人員」引用檢查改查關聯表 | 見下，這是修 bug |
| 4 | `BranchServiceTest:229` 對應改 mock | 1 行 |
| 5 | `UserDto.branchCode` 刪；若查詢要回營業所，改為 `List<String>` 從關聯表組 | 決定要不要回 |

> **第 3 點是真 bug，不只是搬家**：現行檢查查的是 `AuthUser.branchCode`，
> 會漏掉「在該所有角色、但 `branchCode` 是別處」的人——刪掉營業所後留下孤兒 `AuthUserBranchRole`。
> 改查關聯表才擋得住。`location` 那一項（`BranchService:85`）本來就是對的，不用動。

### ⚠ 三個陷阱

**① `UserContextHolder` 改成物件後，`clear()` 一定要跟著改。**
`JwtInterceptor.afterCompletion` 的清理漏了的話，Tomcat 執行緒重用時
會把上一個請求的權限帶給下一個人——這比記憶體洩漏嚴重得多。
（那支檔案裡已有註解解釋為什麼 `setUserCode` 要等到權限檢查通過後才寫，一併看。）

**② 放進 `ThreadLocal` 的 map 必須不可變**（`Map.copyOf`）。
現在存 `String` 天生不可變所以沒事；換成 map 之後，Service 層任何一處都能往自己的權限裡塞東西。

**③ JWT claim 的往返會把 `Set` 變成 `List`。**
`claims.get("branchRoles", Map.class)` 拿回來的是 `LinkedHashMap<String, ArrayList<String>>`；
泛型已被抹除，直接 cast 成 `Map<String, Set<String>>` 會編譯過、執行不炸
（`List` 也有 `contains`），但型別是謊話。解析端要明確重建 `Set`，這是 `JwtUtil` 的責任。

### 完成的定義

- token 解出來能回答「這個人在營業所 X 有沒有角色 Y」
- `AuthServiceTest` 更新——注意 `login_whenValid_shouldReturnTokenAndPrimaryBranchRole`
  測的正是要拆掉的「挑一個」行為，整支重寫而非改參數。新案例至少三筆：
  同一營業所兩個角色、跨營業所不同角色、無任何角色關聯應拋 `AUTH_NO_ROLE_ASSIGNED`
- 比對用 `Set` 比對，別比 token 字串——`HashSet` 序列化順序不保證，那種測試會偶發紅
- `BranchServiceTest` 的營業所刪除案例仍綠（引用檢查換表後，mock 對象跟著換）
- `./mvnw.cmd test` 全綠

> **未決**：`data.sql` 四個使用者目前都是單所單角，多所／同所多角一個案例都沒有。
> 單元測試用 mock 不受影響，但手動 demo 測不到這次改造的重點。
> 要補的話會動到 `location` 資料，進而影響配貨 demo 的數字——做前端時再決定。
>
> 第 3 步之後多了一個連帶影響：**單一帳號已經跑不完主線**（U001 只有 SALES、U002 只有 LEADER、
> U003 只有 WAREHOUSE），手動 demo 要嘛逐段換帳號登入，要嘛全程用 `A001`（ADMIN 通吃）。
> 前端 demo 若想演「不同角色看到不同按鈕」，補多角資料就從這裡開始。

---

## ✅ 已完成：掛 `@RequireRole` 到端點（2026-09-07）

58 支端點全部掛上，含純讀取端點（標四個角色）。定案理由已寫進
`User.md`「標註覆蓋率」與「`/actuator/**` 不在攔截器覆蓋範圍內」兩段，慣例寫進 `backend.md`
「API 慣例」，`RequireRole` 的 javadoc 也補了「未標註視為漏掛」。

### 端點—角色對照表（寫守門測試時對著這張表）

**未標註（1 支）**：`POST /api/auth/login`——已在 `WebMvcConfig` 排除攔截，呼叫時還沒有身分。

**四角色全開（32 支讀取）**：

- `GET /api/users`、`/api/users/{userCode}`
- `GET /api/branches`、`/api/branches/{branchCode}`
- `GET /api/locations`、`/api/locations/{locationCode}`
- `GET /api/products`、`/api/products/{productCode}`
- `GET /api/customers`、`/api/customers/{customerCode}`
- `GET /api/factories`、`/api/factories/{factoryCode}`
- `GET /api/sales-orgs`、`/api/sales-orgs/{salesOrgCode}`
- `GET /api/sales-purchase-orders`
- `GET /api/branch-purchases`
- `GET /api/branch-purchase-orders`
- `GET /api/factory-delivery-orders`、`/pending`、`/received`
- `GET /api/allocation-orders`、`/pending-spod`、`/{allocationNo}`
- `GET /api/sales-receive-orders`、`/pending`、`/{receiveNo}`
- `GET /api/inventory`、`/warehouse/{branchCode}`、`/location/{locationCode}`、
  `/product/{productCode}`、`/transactions`、`/snapshot/{date}`

**有限制（26 支動作與主檔寫入）**：

| 角色 | 端點 | 支數 |
|------|------|:----:|
| `ADMIN` | `POST`／`PUT`／`DELETE` × {`/api/branches`, `/api/products`, `/api/customers`, `/api/factories`, `/api/sales-orgs`} | 15 |
| `ADMIN` | `POST /api/factory-delivery-orders/actions/ship`（Mock 出貨） | 1 |
| `ADMIN` | `POST /api/inventory/snapshot`（手動快照） | 1 |
| `LEADER`, `ADMIN` | `POST /api/branch-purchases/actions/{freeze,unfreeze,confirm}`、`PUT /api/branch-purchases/adjust` | 4 |
| `LEADER`, `WAREHOUSE`, `ADMIN` | `POST /api/branch-purchase-orders/actions/aggregate` | 1 |
| `WAREHOUSE`, `ADMIN` | `POST /api/factory-delivery-orders/actions/receive`、`POST /api/allocation-orders/actions/allocate` | 2 |
| `SALES`, `LEADER`, `ADMIN` | `PUT /api/sales-purchase-orders`（建立 SPO） | 1 |
| `SALES`, `ADMIN` | `POST /api/sales-receive-orders/actions/receive`（領貨） | 1 |

### 待你寫的守門測試

反射掃 `controller` package 全部 `@RequestMapping` 方法，斷言：① 除 login 外每支都有
`@RequireRole`；② 角色集合與上表一致。這是本專案第一支反射／架構測試，依「每一種新東西的第一個
自己寫」的紀律留給你。要點：比對用 `Set` 不比陣列順序；`AuthController.login` 要當成明列的白名單
而不是「沒有標註就跳過」，否則測試本身就 fail-open。

### actuator 的洞：改成不曝光，沒有改攔截範圍

原本這裡寫的修法（`addPathPatterns("/api/**")` → `/**`）**做不到它想做的事**：
`WebMvcConfigurer.addInterceptors` 註冊的攔截器只會被塞進 `WebMvcConfigurationSupport`
自己建的那幾個 handler mapping，actuator 另有一組 `WebMvcEndpointHandlerMapping`，
它只吃 `MappedInterceptor` 型別的 bean（已在 Spring Boot 3.4.1 原始碼確認）。
改 `/**` 只會多攔到自家 `/api/` 以外的路徑，actuator 依然裸奔。

實際做法：`exposure.include` 只留 `health`、`show-details=never`（不是 `when-authorized`——
沒有 Spring Security 就永遠取不到 principal，那個值的行為等於 `never`，寫 never 才不誤導）。

> 因此「攔截器 fail-open」這件事**沒有解決**，只是把當時唯一的受害者關掉了。
> 要根治得改用 `MappedInterceptor` bean 註冊、預設全擋、例外才排除；代價是
> `WebMvcConfig` 不再是 `WebMvcConfigurer`，`@WebMvcTest` 切片就不再載入它，
> 五支 Controller 測試裡的 `when(preHandle).thenReturn(true)` 會變成無效的死 stub，
> 且必須排除 `/error`（否則未登入者的 404 會變 401）。目前所有端點都在 `/api/` 底下，
> 等真的出現非 `/api/` 路徑（webhook、SSE）再改。

## 接著（可以交給 Claude）

### 4. 資料級授權

原則已定：**不信任前端**。呼叫端送來的 `branchCode`／`locationCode` 一律視為不可信輸入，
與 token 身分不符即拒絕。三級範圍與落點（Service 層）見 `User.md`「資料範圍授權」段。

現況是 19 支端點的 branchCode／locationCode 完全由呼叫端自由指定，
任何登入者都能讀別的營業所、別的業務員的單——這是 IDOR，是洞不是缺功能。

**這一步是必要的，不是加分項**——因為第 3 步的 `@RequireRole` 只判定「在任一營業所有此角色」，
營業所比對完全不在攔截器（2026-08-28 定案，理由與端點盤點見 `User.md`）。

收斂成兩個入口，別每支 Service 各寫各的 `if`：

- `assertBranchAccess(branchCode, 需要的角色)`——驗 branchCode 在 token 的 keys 裡，
  **且該所底下有需要的角色**（第二點最容易漏，漏了等於只驗了一半）
- `assertLocationOwnership(locationCode)`——驗 `Location.userCode` = 當前登入者
- **ADMIN 一律放行，兩者都不比對**（全系統唯一的角色特例，見 `User.md`）。
  前端選單也要對應——ADMIN 列全部營業所，須另呼叫 `GET /api/branches`。
  兩邊要一起做：只做前端會讓 ADMIN 選了卻被擋，只做後端則他選不到

每支需要範圍檢查的 Service 方法，都要有一支「別所／別人的儲位打進來要被擋」的測試。

---

## 待拍板的三個小決策

做第 3、4 步時會遇到，先想好可以省一輪。

**D1｜資料一致性守門要 500 還是走 `BusinessException`**
`FactoryDeliveryOrderService:144` 拋 `IllegalStateException`（→500，有測試釘住）；
`AllocationService:115` 對同形狀的守門拋 `BusinessRuleException(PURCHASE_ORDER_NOT_FOUND)`（→409）。
兩處必須統一。Claude 推薦：新增 `DATA_INTEGRITY_VIOLATION`(500) 兩處共用——
語意誠實（DB 壞了不是業務失敗）、走既有例外家族、契約有登錄。

**D2｜`BranchPurchaseController` 四支回 `void` 的端點**
現在是 200 + 空 body，但 `backend.md` 寫「PUT/PATCH → 200 + 資源，或 204」，兩邊都不是。
改 204 最省；freeze/confirm 若前端要拿回凍結狀態則回 DTO。

**D3｜`BranchPurchaseService:119` 靜默 `continue`**
組長對「當天沒有訂單的儲位」做調整時被無聲丟棄，回應是 void 也看不出來。
要拋錯還是照收（自動建 SPO）？`aggregate()` 的「全部驗完才動手」可對照。

---

## 已知但刻意不做的（別再重新發現一次）

| 項目 | 狀態 |
|------|------|
| `findOrCreateInventory` 無鎖 → 收貨 vs 配貨 lost update | 真的洞，但要配階段 4 的併發整合測試一起改；ADR-0013 的「已知缺口」段推論不完整，也要補記 |
| 配貨的「算」與「扣」有時間差，且算的時候讀到的 Inventory 已進 persistence context | 同上，一併處理。修法：給 `findByBranchCodeAndLocationType` 加 `@Lock` |
| `BranchPurchaseOrderService:54` 彙總無鎖，連點可能產生重複 BPO | 同一類，一個 annotation 的事 |
| 401/403 沒有 response body | 前端已有 workaround（「401 且無 body」判為逾時）；403 已隨第 3 步變成真的會發生（例如 SALES 打凍結端點），前端仍無對應處理——做前端時一併補 |
| 手刻 JWT 不用 Spring Security | ADR-0010 有記錄。升級條件：要 OAuth2/SSO、資料級授權、多角色——**三者已觸發兩個**，做完第 2、4 步後值得回頭評估遷移，並寫新 ADR 取代 0010 |
| ADR 統整（補記 0013/0010、補寫 String 業務碼與 ddl-auto 兩篇） | 刻意延後。文件已經跑在程式前面太多，先把程式做完 |
| `data.sql` 改過但沒實際跑過 | 本機沒有 Docker/SQL Server，改號那批只做過靜態檢查。第一次啟動應用程式時留意 INSERT 有沒有失敗 |

---

## 更遠的路線（2–3 個月版本）

1. ~~SRO 領貨~~ ✅
2. **授權**（本文件上半部）← 現在在這
3. **前端 demo 主路徑**——最大一塊。Element Plus + 五頁：訂貨／彙總凍結／收貨／配貨／領貨。
   重點只有一個：**配貨結果頁要讓演算法看得見**（S001 因優先度 1 先拿走效期最近那批，
   S002 只分到剩下的，數字和批號都要在畫面上）。
   每頁都要有**營業所欄位**（已定案：每頁各自的欄位，不做全域切換；一律顯示並自動帶入，
   預設取本頁合法集合中排序最小者，單一選項時鎖定唯讀）——四條規則見
   `User.md`「登入契約與前端營業所選擇」段，其中「選單只列具備本頁所需角色的營業所」最易漏
4. **一鍵部署**——multi-stage Dockerfile（前後端同 image）、compose 加 app service、healthcheck
5. **README 放一張 gif**——這一張圖的效益大於後面十篇 ADR

**明確放掉**：OAuth2／社群登入、階段 5 的 k8s。

**若提前做完**：回頭做階段 4 的併發驗證。那是這專案唯一還沒兌現的核心主張，
而且上表前三項的洞已經確定存在——「寫了併發測試、重現 lost update、修完再測」
寫進 README，價值遠高於任何一個新框架。
