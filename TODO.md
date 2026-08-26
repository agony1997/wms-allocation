# TODO

> **暫時文件，全部做完就刪掉。**給自己看的，不是專案文件——正式的決策紀錄在 `docs/adr/`、
> 規格在 `docs/requirements/specification/`、進度在 `README.md` 的「系統範疇」表。
>
> 隔很久回來時：從「現在在哪」讀起，再跳到「下一步」。
> 要接續工作，跟 Claude 說：**「讀 TODO.md，我要做（某一項）」**。

---

## 現在在哪

主線 **訂貨 → 配貨 → 領貨** 三段後端全部完成，測試 193 綠。

```
07df060 docs: 補齊權限矩陣、定案資料範圍授權與 token 角色時效
b92903b feat: 實作業務領貨單（SRO），補完訂貨→配貨→領貨主線
d769521 feat: 建立倉儲配貨系統後端與專案文件基礎
```

目前卡在**授權**這條線上，分四步，做完才輪到前端。

> 跑測試：`./mvnw.cmd test`
> `BranchRepoTest` 需要 Docker（Testcontainers）。Docker 沒開時它會 error，其餘 191 支照跑。

---

## 下一步：多角色改造 ← **你寫這個**

**為什麼是你寫**：這是專案裡第一個授權相關的實作。依協作紀律，每一種新東西的第一個自己寫，
之後的才交給 AI——沒寫過就審不出 AI 產出哪裡不對。

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
- 完整理由與升級階梯見 `docs/requirements/specification/master/User.md` 的
  「Token 攜帶角色與撤銷時效」段

### 要做什麼

1. `JwtUtil` 簽發／解析 `branchRoles`
2. `AuthService.resolveRole()` → 改為回傳全部角色關聯（不再挑一個）
3. `UserContextHolder` → `userCode` + `Map<branchCode, Set<String>>`，
   並提供類似 `hasRole(branchCode, role)` 的查詢方法
4. `JwtInterceptor` 改為多角色比對
5. `LoginResponse` 的 `role` 欄位要不要跟著改（前端 `stores/auth.js` 有讀）

### ⚠ 陷阱

`UserContextHolder` 改成物件後，**`clear()` 一定要跟著改**。
`JwtInterceptor.afterCompletion` 的清理漏了的話，Tomcat 執行緒重用時
會把上一個請求的權限帶給下一個人——這比記憶體洩漏嚴重得多。
（那支檔案裡已有註解解釋為什麼 `setUserCode` 要等到權限檢查通過後才寫，一併看。）

### 完成的定義

- token 解出來能回答「這個人在營業所 X 有沒有角色 Y」
- `AuthServiceTest` 更新，且至少有一筆「一人在同一營業所有兩個角色」的案例
- `./mvnw.cmd test` 全綠

---

## 接著（可以交給 Claude）

### 3. 掛 `@RequireRole` 到端點

照 `User.md` 權限矩陣掛上 58 支端點。矩陣已補齊、每列都標了依據，這步是機械工作。
**必須等第 2 步做完**，否則掛上去的是錯的（單角色比對會把一人多角的使用者擋掉）。

順便一起修的兩個洞：

- **`@RequireRole` 目前零使用**——整個系統只有「要不要 token」，沒有「你是誰能做什麼」
- **`/actuator/**` 完全沒被攔截器蓋到**（只攔 `/api/**`），而 `show-details=always`
  會把資料庫連線狀態吐給未登入者。修法：`addPathPatterns("/**")` 改成預設全擋、
  例外才 `exclude`（`/api/auth/login`、`/actuator/health` 給 docker healthcheck），
  並把 `show-details` 改成 `when-authorized`

> 這是「攔截器 fail-open」的體現：Spring Security 預設 deny-all，漏掉一條路徑會被擋住、
> 馬上發現；HandlerInterceptor 預設 allow-all，漏掉就是裸奔且無人提醒。

### 4. 資料級授權

原則已定：**不信任前端**。呼叫端送來的 `branchCode`／`locationCode` 一律視為不可信輸入，
與 token 身分不符即拒絕。三級範圍與落點（Service 層）見 `User.md`「資料範圍授權」段。

現況是 19 支端點的 branchCode／locationCode 完全由呼叫端自由指定，
任何登入者都能讀別的營業所、別的業務員的單——這是 IDOR，是洞不是缺功能。

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
| 401/403 沒有 response body | 前端已有 workaround（「401 且無 body」判為逾時），但 403 沒有對應處理，等第 3 步掛 `@RequireRole` 後會浮現 |
| 手刻 JWT 不用 Spring Security | ADR-0010 有記錄。升級條件：要 OAuth2/SSO、資料級授權、多角色——**三者已觸發兩個**，做完第 2、4 步後值得回頭評估遷移，並寫新 ADR 取代 0010 |
| ADR 統整（補記 0013/0010、補寫 String 業務碼與 ddl-auto 兩篇） | 刻意延後。文件已經跑在程式前面太多，先把程式做完 |
| `data.sql` 改過但沒實際跑過 | 本機沒有 Docker/SQL Server，改號那批只做過靜態檢查。第一次啟動應用程式時留意 INSERT 有沒有失敗 |

---

## 更遠的路線（2–3 個月版本）

1. ~~SRO 領貨~~ ✅
2. **授權**（本文件上半部）← 現在在這
3. **前端 demo 主路徑**——最大一塊。Element Plus + 五頁：訂貨／彙總凍結／收貨／配貨／領貨。
   重點只有一個：**配貨結果頁要讓演算法看得見**（S001 因優先度 1 先拿走效期最近那批，
   S002 只分到剩下的，數字和批號都要在畫面上）
4. **一鍵部署**——multi-stage Dockerfile（前後端同 image）、compose 加 app service、healthcheck
5. **README 放一張 gif**——這一張圖的效益大於後面十篇 ADR

**明確放掉**：OAuth2／社群登入、階段 5 的 k8s。

**若提前做完**：回頭做階段 4 的併發驗證。那是這專案唯一還沒兌現的核心主張，
而且上表前三項的洞已經確定存在——「寫了併發測試、重現 lost update、修完再測」
寫進 README，價值遠高於任何一個新框架。
