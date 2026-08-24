# 11. 祕密改由環境變數注入，不寫進版控

日期：2026-07-03

## 狀態

已接受 (Accepted)

## 背景

引入手刻 JWT 驗證（見 [ADR-0010](0010-custom-jwt-auth-without-spring-security.md)）後，多了一個 JWT 簽章密鑰需要保管；加上原本就寫死在 `application.properties`（受版控）的 SQL Server `sa` 密碼，正好一併重新檢視祕密的擺放。

面臨的選擇：

1. **繼續寫死在受版控的設定檔**：dev 零摩擦、clone 下來就能跑。但祕密會進 git 歷史——即使事後改掉，歷史與 fork 仍留存；哪天 repo 公開、或這個 pattern 被複製到真專案，祕密就外洩。
2. **外置到環境變數**：設定檔只保留 `${VAR}` 佔位，實際值放不進版控的 `.env`，另備 `.env.example` 進版控當「需要哪些變數」的契約。

本專案雖是學習性質、`sa` 密碼也只保護本機容器，但 JWT 密鑰對應的是真實 app 祕密，值得把正確習慣建立起來，順帶把這套機制實際跑一遍。

## 決策

我們將**所有祕密外置到環境變數**：

- `.env`（gitignore）存實際值，為 docker-compose 與 app 共用的單一來源；`.env.example`（進版控）記錄需要哪些變數與產生方式。
- `application.properties` 的 `spring.datasource.password`、`jwt.secret` 改讀 `${DB_PASSWORD}`、`${JWT_SECRET}`，**不設 fallback**：未提供即啟動失敗，強制外部提供。
- `docker-compose.yml` 的 `sa` 密碼改 `${DB_PASSWORD}`，由 compose 原生讀取 `.env` 代入。
- 連本機 dev DB 密碼也一起外置：它其實可放寬（很多團隊直接 commit 一個 dev 預設），但只藏 JWT 密鑰、DB 密碼照樣寫死會顯得半套，故一致處理。

## 後果

**正面影響**：
- 祕密不進版控；輪替密鑰不需改程式碼、不需重新 commit。
- `.env.example` 是明確契約，新人 clone 後知道要設哪些變數。
- 與 CI（平台祕密庫）、正式環境（雲端祕密管理服務）是同一套心智模型的延伸。

**負面影響 / 要承擔的事**：
- **Spring 不會自動讀 `.env`**：每台開發機、每種跑法都要把值放進環境（IntelliJ 的 Run Configuration，或 pwsh 先載入 `.env` 再啟動），新人多一步；忘了設會啟動失敗。操作見 [docs/development.md](../development.md)。
- 舊值仍留在 git 歷史：JWT 密鑰已換成全新隨機值、舊的作廢無妨；`sa` 密碼未變動且只是本機 dev 容器密碼，風險低，故不做歷史清除（真需要時用 BFG / git filter-repo）。
- 若日後嫌每台機器手動設環境變數麻煩，可引入 `spring-dotenv` 讓 Spring 比照 docker-compose 直接讀 `.env`，代價是多一個依賴、且值的來源變隱式。
