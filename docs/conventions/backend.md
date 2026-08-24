# 後端慣例

根 package：`com.agony.wmsallocation`。採傳統三層架構（決策與背景見 [ADR-0002](../adr/0002-adopt-traditional-three-layer-architecture.md)）。

## Package 結構

```
com.agony.wmsallocation
├── config/          # 基礎設定（JpaAuditingConfig、SchedulingConfig 等）
├── security/        # 自訂 JWT 驗證（攔截器、@RequireRole、UserContextHolder）
├── controller/      # REST Controller
├── dto/             # 請求／回應 DTO，按業務領域分子目錄
├── mapper/          # MapStruct Entity ↔ DTO 轉換
├── service/         # 業務邏輯
├── repository/      # Spring Data JPA Repository
├── exception/       # ErrorCode、BusinessException 家族、GlobalExceptionHandler
└── entity/          # Entity + enum，按業務領域分子目錄
    ├── AuditEntity.java
    └── auth/ branch/ purchase/ receive/ inventory/
        master/ sequence/ allocation/
```

依賴方向：`controller → service → repository → entity`，禁止反向依賴。`dto/` 與 `mapper/` 供 controller／service 使用，`exception/`、`security/`、`config/` 為橫切關注點，不屬於這條鏈。

領域子目錄與業務的對照見 [docs/domains.md](../domains.md)。

## Entity 規範

- **必須**繼承 `AuditEntity`（提供 createdAt/updatedAt/createdBy/updatedBy）。例外：`DocumentSequence`（純技術序號表，不掛審計欄位）
- JPA Auditing 已啟用（`config/JpaAuditingConfig`），不需手動設定 audit 欄位；auditor 為當前登入使用者，無登入情境（排程、測試）回退為 `SYSTEM`
- Entity 為**純資料類別**（anemic model）：只有欄位 + Lombok getter/setter，業務邏輯一律寫在 Service
- **PK 一律 `Integer id + @GeneratedValue(IDENTITY)`**，業務碼另立 `@Column(unique = true, nullable = false)` 欄位。例外：`DocumentSequence` 採複合 PK（`sequenceType + sequenceDate`，序號表以業務鍵唯一識別）
- **跨表引用一律用 String 業務碼**（如 `productCode`、`branchCode`），不用 `@ManyToOne`/`@OneToMany`。影響：JPA 不產生 FK constraint，資料一致性由應用層負責；需 JOIN 時手寫 JPQL 或 Native SQL
- **單據明細冗餘的 `productName` 是有意的非正規化（單據快照）**：建立單據時從 `Product` 複製，之後不隨主檔異動，勿當成同步 bug 修
- 領域 enum 與 Entity 放在同一個 `entity/{領域}/` 子目錄
- **封閉分類值用 enum、不建資料表**：判斷準則是「新增一個值是否需要跟著寫新程式碼」——會（單據類型、狀態機狀態）→ enum + `@Enumerated(EnumType.STRING)` 直存字串；不會（業務可自行新增，如商品分類）→ 才建資料表。理由見 [ADR-0007](../adr/0007-closed-code-bound-sets-as-enum.md)

## API 慣例

- 所有端點以 `/api/` 為前綴
- 回應語言使用繁體中文

### 回應設計（RESTful 務實派，全站一致）

採「成功回資源本體 + HTTP status 表達結果，失敗才結構化」的風格，**不使用統一信封（envelope）**。決策背景、被否決的替代方案與升級條件見 [ADR-0003](../adr/0003-restful-response-without-envelope.md)。

- **成功**：直接回傳資源本體（DTO），搭配對應 HTTP status
  - `GET` → `200` + DTO / `List<DTO>`
  - `POST` 建立 → `201 Created` + 新建資源（或 `Location` header）
  - `PUT`/`PATCH` 更新 → `200` + 更新後資源，或 `204 No Content`
  - `DELETE` → `204 No Content`，body 留空
- **失敗**：一律由 `GlobalExceptionHandler` 統一回傳 `ErrorResponse`，搭配對應 HTTP status。新增 `ErrorCode` 時**必須同步**更新錯誤碼對照表 [docs/api/error-codes.md](../api/error-codes.md)（前後端契約的單一真相來源）；既有 code 改名視為破壞性變更
- **禁止**：Controller 直接回傳 Entity（一律經 DTO）；全部回 `200` 再靠 body 內 code 判斷成敗
- **一致性原則**：所有動詞用同一套規則（HTTP status 表達結果）。「刪改回空 body」是 HTTP 語意，不算不一致；真正要避免的是「同一件事用兩種不同的回應結構」（例如查詢包信封、刪改不包）

## 測試慣例

策略的「為什麼」見 [ADR-0005](../adr/0005-testing-strategy-by-uncertainty.md)，操作重點：

- **依不確定性選工作流**：機械、已知形狀的功能（主檔 CRUD）走 **test-after**（先實作再補測試）；有真實邏輯、設計未定的業務規則走 **test-first（TDD）**。
- **測在邏輯所在層**：
  - **Service**：主要投資點，純 Mockito（`@ExtendWith(MockitoExtension.class)`），釘業務分支（重複／找不到／下轄檢查／軟刪／預設值／身份不可改）。
  - **Controller**：想釘 `@Valid` 與 status code 契約時，每主檔挑代表性的寫；錯誤格式已由 `GlobalExceptionHandlerTest` 集中測，不重複。
  - **Repository**：僅在有自訂 `@Query` 或不直觀衍生查詢時寫（需 Testcontainers，成本高，保持稀少）。
  - **Mapper**：僅在有非平凡 `@Mapping` 時寫。

### 需要時再升級（避免過度設計，現在不要先做）

- **分頁**：當清單查詢需要分頁時，再引入 Spring Data 的 `Page<T>` / 專用 `PageResponse`（附 total/pageNumber），不要預先包裝
- **統一信封**：僅當出現明確需求（例如前端要求「永遠 200、看 body 內 code」、或多端/第三方共用契約）才評估改為 `ApiResponse<T>` 信封，且須**全站一次性切換**
- **traceId / RFC 7807 Problem Details / HATEOAS**：等真有分散式追蹤或對外開放平台需求再加

> 升級屬**需求驅動**，非臆測。變更回應風格前須更新本段並全站貫徹（並補一篇取代 [ADR-0003](../adr/0003-restful-response-without-envelope.md) 的新 ADR）。
