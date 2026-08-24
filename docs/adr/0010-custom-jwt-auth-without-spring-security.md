# 0010. 採用手刻 JWT 驗證，不引入 Spring Security

日期：2026-07-03

## 狀態

已接受 (Accepted)

## 背景

本專案 (`wms-allocation`) 是一個供學習與模擬展示使用的前後端分離架構系統 (Spring Boot + Vue)。
在設計身分驗證 (Authentication) 與權限控管 (Authorization) 機制時，我們面臨兩個主要選擇：

1. **Spring Security + JWT**：業界標準做法，具備完備的防護機制（如 CSRF 防護、Security Headers）與豐富的註解支持（`@PreAuthorize`）。缺點是學習曲線陡峭，其 Filter Chain 內部實作複雜，初學者容易在除錯上受挫。
2. **純手寫 JWT 驗證 (Spring Web Interceptor + ThreadLocal)**：不依賴 Spring Security 框架，自行實作 Token 簽發、解析，並透過 `HandlerInterceptor` 攔截請求，再搭配自訂標籤 (如 `@RequireRole`) 控制 API 權限。

考量到本專案的學習性質，以及業務角色權限相對單純（僅分為業務員、組長、庫務，主要進行 API 的分層阻擋，無複雜的細粒度 Method Security 需求）。

## 決策

我們決定**不引入 Spring Security**，而是採用**純手寫 JWT 驗證**機制：

1. 依賴：僅引入 `jjwt` 處理 Token 簽發與解析。
2. 攔截：透過實作 `HandlerInterceptor` (例如 `JwtInterceptor`) 攔截前端帶有 `Authorization: Bearer <token>` 的請求。
3. 狀態儲存：解析後的登入者資訊 (`userCode`, `role`) 存入自製的 `UserContextHolder` (`ThreadLocal`)，供業務邏輯存取。
4. 權限控管：自行定義 `@RequireRole` 標籤，在 Interceptor 層級直接判斷權限，不符則回傳 403 Forbidden。

## 後果

**正面影響**：
- 程式碼透明度極高，所有的驗證與授權邏輯都在開發者可見、可輕易斷點除錯的範圍內。
- 初學者能透過實作過程，透徹理解 JWT 的運作原理與 HTTP 驗證底層邏輯。
- 避開了 Spring Security 繁重的設定，讓專案保持輕量。

**負面影響**：
- 犧牲了 Spring Security 內建的安全防護機制（例如預設的 Security Headers，必須自行處理或承擔這部分風險）。
- 若未來業務變得極度複雜，需要細粒度的資料級別權限控管 (如需 SpEL 表達式動態授權) 時，手刻的 `@RequireRole` 將不足以支撐，需手動寫很多 if/else，或最終仍需遷移至 Spring Security。但這對當前的 Mock 專案而言是可以接受的妥協。
