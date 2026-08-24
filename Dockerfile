# =============================================================================
# 應用程式執行期映像
#
# 設計：直接放入 Jenkins 已經 build + test 過的 jar，不在這裡重新編譯。
#       (CI 階段已測過，這裡只負責「跑」，映像小、建置快)
# =============================================================================

# 只需 JRE(執行環境)即可跑 jar，不需完整 JDK，映像較小
FROM eclipse-temurin:17-jre

WORKDIR /app

# Jenkins 的 Package 階段會在 target/ 產出 jar；用萬用字元免寫死版本號
COPY target/*.jar app.jar

# Spring Boot 預設埠(application.properties: server.port=8080)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
