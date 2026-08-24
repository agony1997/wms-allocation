# 3. RESTful 回應不使用統一信封

日期：2026-06-29

## 狀態

已接受 (Accepted)

## 背景

設計 API 回應格式時，常見兩種風格：

- **統一信封（envelope）**：所有回應都包成 `{ code, message, data }`，HTTP status 多半固定 `200`，靠 body 內的 `code` 判斷成敗。前端解析統一，但與 HTTP 語意脫節、查詢需多剝一層、status 失去意義。
- **RESTful 務實派**：成功直接回資源本體 + 對應 HTTP status，失敗才回結構化錯誤。貼近 HTTP 語意、查詢直接可用，但前端需處理多種 status 與兩種 body 形狀。

本專案前端待重建（規劃 Vue 3），目前沒有「前端強制要求永遠 200」或「多端/第三方共用契約」這類驅動信封的明確需求。

## 決策

我們將採用 **RESTful 務實派、不使用統一信封**，且全站一致：

- **成功**：直接回傳資源本體（DTO）+ 對應 HTTP status（`GET`→200、`POST`→201、`PUT/PATCH`→200 或 204、`DELETE`→204 空 body）。
- **失敗**：一律由 `GlobalExceptionHandler` 回傳 `ErrorResponse` + 對應 HTTP status。
- **禁止**：Controller 直接回傳 Entity（一律經 DTO）；全部回 200 再靠 body 內 code 判斷成敗。
- 錯誤碼以 [docs/api/error-codes.md](../api/error-codes.md) 為前後端契約的單一真相來源；新增 `ErrorCode` 必須同步更新，既有 code 改名視為破壞性變更。

操作細節見 [docs/conventions/backend.md](../conventions/backend.md)。

## 後果

- 回應貼近 HTTP 語意，查詢類端點 body 乾淨、可直接使用。
- 前端需處理多種 HTTP status 與「成功回資源 / 失敗回 ErrorResponse」兩種形狀；以 `GlobalExceptionHandler` 集中錯誤結構來降低不一致風險。
- **升級是需求驅動而非臆測**：分頁（`Page<T>`）、統一信封、traceId / RFC 7807 / HATEOAS 等都等真有需求才加。其中改用統一信封屬破壞性變更，須全站一次性切換，並補一篇取代本 ADR 的新 ADR。
