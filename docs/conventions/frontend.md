# 前端慣例

沿用已刪除的舊前端留下的可用慣例重建，逐一確認過仍合理才繼續採用。技術棧：Vue 3 + Vite + Vue Router + Pinia + axios。

UI 元件庫：目前先不用，畫面用原生 HTML（`<form>`/`<table>`）+ 手寫 CSS。→ 等可編輯儲存格、跨頁篩選這類複雜表格需求出現、手刻明顯划不來時導入。**評估已定案：屆時導入 Element Plus**（預期掛載點：配貨/庫存頁，見 [README](../../README.md) 階段 2），導入時補升級 ADR 並更新本段。

## 目錄結構

```
frontend/src/
├── main.js          # bootstrap：Pinia、Router
├── App.vue          # 根元件，目前只有 <router-view />
├── style.css         # 全站最小 CSS reset + 基礎字型
├── api/              # 依領域分檔的 API 呼叫（httpClient.js 是共用 axios 設定，不算領域檔）
├── stores/           # Pinia store，只放需要跨元件共享的狀態
├── router/
│   └── index.js      # 唯一的路由設定檔，路由表與登入守衛都在這裡
└── views/            # 頁面元件，扁平放在這一層，用檔名前綴區分領域
```

依賴方向：`views → stores → api → httpClient`，不可反向依賴（`api/` 底下的檔案不可以 import `stores/` 或 `router`）。

## API 呼叫慣例

- 一律用 axios，不用原生 fetch。所有 axios 設定集中在 `src/api/httpClient.js`，其餘 api 檔案只從這裡 import `httpGet`/`httpPost`/`httpPut`/`httpDelete`。
- 依領域分檔：`src/api/auth.js`、`src/api/branch.js`、未來的 `src/api/customer.js`...，每個檔案對應後端一個 controller 領域，一支後端 API 對一個匯出函式。
- `httpClient` 的 `baseURL` 已經是 `/api`：呼叫時路徑**不要**再加 `/api` 前綴。
- `httpClient` 已解包回應：成功直接拿到 `response.data`（`204` 回 `null`）；失敗用 `try/catch`，接到的是後端 `ErrorResponse` 或 `Error`，兩者都有 `.message` 可直接顯示。
- 為什麼 `httpClient.js` 不 import `router`：會跟 `stores/auth.js`（依賴 `api/auth.js` → `api/httpClient.js`）繞成循環引用，所以 401 逾時導頁用 `window.location.href`，不用 `router.push()`。

## 狀態管理慣例

- 用 Pinia，寫成 setup store（`defineStore('xxx', () => {...})`），跟 `stores/auth.js` 風格一致。
- 需要「重新整理不掉狀態」的 store，在 setup 函式本體內同步讀 `localStorage` 完成初始化，不要另外寫 `init()`。
- 只有「跨元件共享」的狀態才進 store；單一頁面內部的表單狀態、loading 旗標留在該元件的 `ref`/`reactive` 就好。

## 路由與登入守衛慣例

- 只有一個 `src/router/index.js`，路由表用 `meta.requiresAuth` 標記。
- 全域 `beforeEach` **內部**才呼叫 `useAuthStore()`：`router/index.js` 會在 `main.js` 執行 `app.use(createPinia())` 之前就被 import 並求值，此時呼叫 `useAuthStore()` 會因沒有作用中的 Pinia instance 而丟例外。
- 目前只判斷「有沒有 token」，沒有角色守衛（後端也還沒有任何 controller 套用 `@RequireRole`，等後端真的限制角色時再一起加）。

## 錯誤處理原則

- [docs/api/error-codes.md](../api/error-codes.md) 是前後端錯誤碼單一真相來源，新增 errorCode 時該文件會同步更新。
- 預設：99% 情況直接顯示 `err.message`，不用管 `errorCode`。
- 例外：只有 error-codes.md「前端是否分支」欄標「是」的碼（如各主檔 `*_CODE_DUPLICATED` 要標紅欄位、`*_HAS_DEPENDENTS` 要提示原因），才需要 `switch (err.errorCode)` 特別處理，動手前先查該文件確認碼名稱。
- 401 的兩種情況已經在 `httpClient.js` 統一處理，元件層級不用再判斷：登入密碼錯誤（`AUTH_BAD_CREDENTIALS`）走一般 `catch` 顯示 message；已登入但 token 失效，攔截器會自動清空並整頁導回 `/login`。

## 表單驗證慣例

- 目前沒有 UI 元件庫，必填/型別這類基本驗證優先用原生 HTML5 屬性（`required`、`type="email"` 等），不用手刻或引入驗證套件。
- 需要跨欄位的商業規則驗證（例如「結束日期不能早於開始日期」）才在 `handleSubmit` 裡手動判斷。

## 新增業務頁面時的慣例

1. 先看後端該領域的 controller/DTO 原始碼確認 API 路徑與欄位名稱，不要用猜的。
2. 在 `src/api/{領域}.js` 新增對應函式，一支後端 API 對一個函式，函式名用「動詞+資源」。
3. 需要跨頁共享狀態才在 `src/stores/{領域}.js` 新增 store。
4. 在 `src/views/` 新增 `{領域}{用途}View.vue`，扁平放。
5. 在 `src/router/index.js` 加一條路由，記得補 `meta.requiresAuth: true`。
6. 資料密集的表格/表單先用原生 `<table>`/`<form>`；等可編輯儲存格、跨頁篩選這類複雜度出現，手刻明顯划不來時，再評估導入元件庫。

## 部署慣例

- 開發：`npm run dev` 起 Vite dev server（`:5173`），`/api` proxy 轉發到 `http://localhost:8080`。
- 正式：`npm run build` 輸出到 `../src/main/resources/static`，跟後端包成同一個 Spring Boot jar 一起部署，同源不需要 CORS。`httpClient.js` 的 `baseURL: '/api'` 兩種情境都直接可用。
