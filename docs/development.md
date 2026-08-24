# 開發指令

## 環境變數（祕密設定，第一次 clone 必做）

DB 密碼與 JWT 密鑰不寫進版控，改由環境變數注入。專案根目錄的 `.env` 是單一來源：

```bash
cp .env.example .env    # 複製範本，填入實際值
```

- `DB_PASSWORD`：SQL Server sa 密碼。**docker-compose 與 Spring 共用同一個值**（compose 建容器、Spring 連線）。
- `JWT_SECRET`：JWT 簽章密鑰，至少 32 bytes。產生：`openssl rand -base64 32`。

**docker-compose** 會自動讀 `.env`，不需額外動作。
**Spring 應用不會自動讀 `.env`**，啟動前需把值放進環境變數，否則 `${DB_PASSWORD}` / `${JWT_SECRET}` 解析失敗、啟動報錯：

- **IntelliJ**：Run → Edit Configurations → Environment variables 填入這兩個；或裝 EnvFile 外掛指向 `.env`。
- **pwsh CLI**：先把 `.env` 載進當前 session 再啟動：

  ```powershell
  Get-Content .env | Where-Object { $_ -match '^\s*[^#].+=' } | ForEach-Object {
      $name, $value = $_ -split '=', 2
      Set-Item "env:$($name.Trim())" $value.Trim()
  }
  mvnw.cmd spring-boot:run
  ```

## 後端（在專案根目錄）

```bash
mvnw.cmd compile            # 編譯
mvnw.cmd test               # 執行測試
mvnw.cmd spring-boot:run    # 啟動應用 http://localhost:8080（需先設好環境變數，見上）
```

> **注意**：Windows 環境用 `mvnw.cmd`（Linux/macOS 用 `./mvnw`）。Wrapper 鎖定 Maven 3.9.16（見 `.mvn/wrapper/maven-wrapper.properties`），首次執行會自動下載。

## Docker（資料庫）

```bash
docker-compose up -d   # 啟動 SQL Server（mssql-init 會自動建立資料庫）
```

- SQL Server: localhost:1433, DB: `wms_allocation`, user: `sa`, password: 見 `.env` 的 `DB_PASSWORD`

## 資料初始化

- `ddl-auto=create`：每次啟動刪除並重建所有資料表。學習專案的刻意取捨（Entity 仍在變動期，省掉 migration 檔的維護成本），**不是待修問題**；正式環境應改 `validate` + migration 工具
- `data.sql`：每次啟動自動插入測試資料
