# Troubleshooting: Testcontainers 無法連接 Docker (Windows 環境)

## 問題描述

在 Windows 環境下使用 Testcontainers 執行測試時，可能會遇到以下錯誤，導致測試無法啟動：

```text
org.testcontainers.containers.ContainerFetchException: Can't get Docker image: RemoteDockerImage...
```

或者在初始化連線時拋出：

```text
NotFoundException (Status 404: {"message":"Not Found"})
```

甚至在底層出現 `400 BadRequestException` 等無法找到有效 Docker 環境（`Could not find a valid Docker environment`）的錯誤。

## 根本原因 (Root Cause)

這是一個由 **Docker Desktop 升級導致的破壞性變更 (Breaking Change)**：

1. **Docker Engine v29.1+**：Docker 官方在 v29.1 版本之後，因安全性與架構考量，正式**移除了對 1.44 版以前的舊版 API 的支援**。
2. **Testcontainers / docker-java 客戶端**：Testcontainers 底層依賴的 `docker-java` 函式庫，預設可能會嘗試使用較舊版本（例如 1.24）的 API 進行連線協商。
3. **衝突結果**：當 `docker-java` 用舊版 API 請求 Docker Desktop 時，Docker 會直接拒絕請求並回傳 `HTTP 400 Bad Request`，導致 Testcontainers 誤判為無法連線至 Docker 環境或 Socket 損毀。

## 解決方案

要解決這個問題，必須強迫 `docker-java` 使用 Docker Desktop 能夠接受的新版 API，同時升級 Testcontainers 到較新版本以獲得最佳的相容性。

### 步驟一：升級 Testcontainers 版本

修改專案根目錄的 `pom.xml`，強制指定較新版本的 Testcontainers（例如 `1.21.4` 或以上）。Spring Boot 3.4.1 預設提供的 1.20.4 版在此情境下會有相容性問題。

在 `<properties>` 區塊加入或修改：

```xml
<properties>
    <java.version>17</java.version>
    <mapstruct.version>1.6.3</mapstruct.version>
    <testcontainers.version>1.21.4</testcontainers.version> <!-- 升級 Testcontainers -->
</properties>
```

### 步驟二：強制指定 docker-java 的 API 版本

在**使用者的家目錄**（Home Directory，Windows 為 `%USERPROFILE%`、Linux/macOS 為 `~`）下，建立一個名為 `.docker-java.properties` 的檔案。

檔案內容加入以下設定：

```properties
api.version=1.44
```

這個全域設定檔會強迫所有底層呼叫 `docker-java` 的 Java 應用程式（包含 Testcontainers），固定以 v1.44 版本的 API 來與 Docker Desktop 的 Named Pipe (`npipe:////./pipe/docker_cli`) 進行通訊，從而避開 400 錯誤。

---

## 附註：測試結束時的 HikariCP Warn 錯誤與 Surefire 30 秒卡頓

當上述問題修復、測試能夠正常連接 MS SQL 容器後，你可能會在測試成功跑完（印出 `BUILD SUCCESS`）並準備結束時，在 Console 看到類似以下的錯誤，並導致 Maven 卡住大約 30 秒：

```text
[SpringApplicationShutdownHook] WARN  com.zaxxer.hikari.pool.PoolBase - HikariPool-1 - Failed to validate connection ConnectionID:66 ...
[ERROR] Surefire is going to kill self fork JVM. The exit has elapsed 30 seconds after System.exit(0).
```

### 為什麼會這樣？
**這個錯誤是完全無害的。**
原因是 Testcontainers 關閉資源的速度極快，在所有測試跑完後，它第一時間就把 MS SQL 容器從 Docker 中刪除了。然而，同一時間 Spring Boot 也在觸發關閉掛鉤（Shutdown Hook），當 Spring Boot 的資料庫連線池（HikariCP）試圖優雅地關閉殘留連線時，發現資料庫已經憑空消失（網路連線中斷），於是發出警告並觸發了預設的 TCP Timeout 等待。這導致 JVM 無法立刻關閉，Maven Surefire 等了 30 秒不耐煩後，便直接將該 JVM 行程強制刪除。

### 如何改善（可選）
如果覺得每次測試完等待 30 秒太久，可以在測試用的 `application.properties` (或 `application-test.properties`) 加上較短的 timeout 設定：

```properties
spring.datasource.hikari.connection-timeout=2000
spring.datasource.hikari.validation-timeout=1000
```
以此縮短連線池在關閉時發現斷線的等待時間，讓 JVM 能提早正常退出。
