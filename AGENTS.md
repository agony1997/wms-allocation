# AGENTS.md

> 給 AI coding agent 的專案規範（Claude Code 經由 CLAUDE.md 的 `@AGENTS.md` 載入；Antigravity 直接讀取本檔）。

## 絕對約束

- **禁止自動 push**：`git push` 一律由使用者明確指示後才執行。
- **回應語言**：繁體中文。
- **範疇界線**：當前實作範疇是**訂貨 → 配貨 → 領貨出庫**。客戶銷售、送貨簽收、寄庫退庫、結帳對帳**已寫規格但暫緩實作**——`docs/requirements/specification/` 下看得到 delivery／closing 規格、`InventoryService` 也有 `sell`／`customerReturn`／`keep` 等尚無呼叫者的方法，**這些都不是待辦訊號**。要動它們先問。

## 專案一句話

`wms-allocation` — 倉儲配貨系統後端。Spring Boot 3.4.1 + Java 17 + SQL Server，傳統三層架構（[ADR-0002](docs/adr/0002-adopt-traditional-three-layer-architecture.md)）；前端 Vue 3 只做 demo 主路徑。root package `com.agony.wmsallocation`。

## 開工程序

1. 讀 [README.md](README.md) 的「系統範疇」表掌握進度——**禁止用 git log 推測進度**。
2. 找到本次任務的領域規格：`docs/requirements/specification/{領域}/`。
3. 涉及錯誤處理 → 讀 [docs/api/error-codes.md](docs/api/error-codes.md)；涉及測試或 Entity → 讀 [docs/conventions/backend.md](docs/conventions/backend.md)。

## 協作原則

- 代寫與否視情況判斷，紀律是：**每一種新東西的第一個由使用者自己寫，第二個之後可交給 AI**。沒寫過就審不出 AI 產出哪裡違反慣例，而「人類負責抉擇」靠的正是那份判斷力。判斷不出是第一個還是第 N 個時**問**。
- 沒有明確的「幫我實作」，**不要逕自產出產品程式碼**。文件同步、規格回寫、ADR、測試資料除外，可直接做。
- **規格定案、架構決策、技術取捨一律由使用者拍板**。AI 提案必須附兩邊代價：失去哪些自動機制（lifecycle／審計／驗證）、未來要改幾處、與既有同類程式碼是否一致（指出對照的 class/ADR），以及明確的推薦方案——第一次提出就列齊，不等追問。
- 解釋時優先講「為什麼」，對照既有 ADR／慣例講。
- 察覺建議與慣例／ADR 衝突時，以文件為準並要求引用出處。

## 實作鐵律（歷史上真實踩過的坑）

| # | 規則 | 為什麼 |
|---|------|--------|
| 1 | 業務錯誤一律拋 `BusinessException` + `ErrorCode`；**禁止**用 `IllegalArgumentException`／`IllegalStateException` 表達業務失敗 | 沒有對應 handler，會落到兜底變 500 |
| 2 | 新增 `ErrorCode` 時，**同一個 commit 內**同步 `docs/api/error-codes.md`；改既有 code 名 = 破壞性變更，先問 | 該檔是前後端契約的單一真相來源 |
| 3 | 會存中文的欄位加 `@Nationalized` | 曾經存進問號 |
| 4 | Entity 規範（繼承 `AuditEntity`、`Integer id + IDENTITY`、業務碼另立 unique 欄、跨表引用只用 String 業務碼、禁 `@ManyToOne`）依 [backend.md](docs/conventions/backend.md)，現況 100% 遵守 | 不要當第一個破例的人 |
| 5 | 測試分流：主檔 CRUD → test-after；真實業務規則（演算法、狀態機、庫存） → test-first；測在邏輯所在層（Service 為主） | [ADR-0005](docs/adr/0005-testing-strategy-by-uncertainty.md) |
| 6 | 動了 Entity 欄位 → 檢查對應規格檔「結構重點」段；動了端點 → 檢查規格「API 設計」段 | 規格採「欄位以 Entity 為準」，但敘述段仍會漂移 |
| 7 | 新單據要取號 → 用 `SequenceService`，勿自造；新分類值判斷「加值要不要改程式」決定 enum vs 資料表 | [ADR-0006](docs/adr/0006-sequence-single-pessimistic-lock.md) / [ADR-0007](docs/adr/0007-closed-code-bound-sets-as-enum.md) |
| 8 | 回應風格：成功回資源本體 + HTTP status，失敗走 `GlobalExceptionHandler`；**不要**引入信封、分頁包裝、traceId | [ADR-0003](docs/adr/0003-restful-response-without-envelope.md) |
| 9 | 完成一個功能後更新 **README「系統範疇」表的狀態欄** | 進度只有這一處記載，不更新就失效 |

## 完工 checklist

- [ ] `mvnw.cmd test` 全綠（不准跳過失敗測試交差）
- [ ] error-codes.md 同步（若動了 ErrorCode）
- [ ] 規格檔同步（若動了 Entity 結構或端點能力）
- [ ] README 範疇表狀態欄更新（若完成了一段）
- [ ] commit 訊息繁中 + conventional prefix；**絕不自動 push**

## 必須停下來問的情況

1. 規格之間矛盾、或規格沒寫到的業務行為——**不要自行腦補規格**。
2. 想新增依賴、新框架、或繞過既有慣例。
3. 想改回應風格、重新命名既有 `ErrorCode`、刪除任何檔案／欄位／文件段落。
4. `git push`、部署、對外服務操作。

## 不要「修」的清單（看起來像問題，其實是有意取捨）

`ddl-auto=create`、無 Spring Security（[ADR-0010](docs/adr/0010-custom-jwt-auth-without-spring-security.md)）、String 業務碼無 FK、單據明細的 `productName` 快照、回應無信封、取號冷啟動缺口（[ADR-0008](docs/adr/0008-defer-sequence-cold-start-gap.md)）、SPO lazy create（[ADR-0009](docs/adr/0009-lazy-create-sales-purchase-order.md)）、`InventoryService` 中暫無呼叫者的異動方法、`SequenceType` 中送貨／結帳單據的單號類型常數（皆為暫緩範疇備用，見「絕對約束」）。想動任何一項，先讀對應 ADR 再提案。

## 索引

| 需要做什麼 | 看這裡 |
|------------|--------|
| 進度、範疇、階段目標 | [README.md](README.md) |
| 後端慣例：package 結構、Entity 規範、API 與測試 | [docs/conventions/backend.md](docs/conventions/backend.md) |
| 前端慣例與新頁面 SOP | [docs/conventions/frontend.md](docs/conventions/frontend.md) |
| 錯誤碼對照（前後端契約） | [docs/api/error-codes.md](docs/api/error-codes.md) |
| 業務領域對照與流程 | [docs/domains.md](docs/domains.md) |
| 架構決策的「為什麼」 | [docs/adr/](docs/adr/) |
| 業務規格（按領域） | [docs/requirements/specification/](docs/requirements/specification/) |
| 編譯／測試／啟動／Docker | [docs/development.md](docs/development.md) |
| 已知環境雷（Testcontainers／Docker） | [docs/troubleshooting/](docs/troubleshooting/) |
