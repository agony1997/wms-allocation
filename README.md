# wms-allocation

批次效期、業務員優先度、庫存併發控制——一套倉儲配貨系統的後端實作。

## 這是什麼

模擬經銷體系的**訂貨 → 配貨 → 領貨出庫**主線。題材選擇是刻意的：配貨這件事同時包含一個非平凡的分配演算法（FEFO × 業務員優先度 × 批次拆分）與一個真實的併發正確性問題（多業務員同時配貨、庫存不能超賣），這兩者是 CRUD 練習長不出來的東西。

一個 side project，沒有真實使用者；目的是把一套完整可運行的業務系統做到能撐得起技術決策的討論。

技術棧：Spring Boot 3.4.1 / Java 17 / SQL Server / JPA，傳統三層架構；前端 Vue 3（僅供 demo 主路徑操作）。

## 系統範疇

一條完整鏈路，終點是「貨到業務員車上」：

| 段 | 內容 | 狀態 |
|----|------|:----:|
| **訂貨** | 業務員訂貨（SPO）→ 門市彙總（BPF/BPO）→ 工廠出貨（FDO）→ 收貨入庫 | ✅ |
| **配貨** | 待配明細撈取 → FEFO × 優先度分配 → 建配貨單（AO/AOD）＋ 扣庫存 ＋ 明細轉態，單一交易 | ✅ |
| **出庫** | 領貨單（SRO/SROD）：庫存由大庫移轉至業務員車存 | 🚧 僅 Entity |

**暫緩（規格已寫、不在當前實作範疇）**：客戶銷售與送貨簽收（CPO/SDO）、寄庫退庫與結帳（SKR/SRR/BRO）。規格保留在 `docs/requirements/specification/` 下，Entity 未建；`InventoryService` 已備妥對應的庫存異動方法（`sell`／`customerReturn`／`keep` 等）。

先做完這三段的理由：它們已經涵蓋本專案想證明的兩件事——**演算法正確性**與**併發正確性**。銷售與帳務是另一組 CRUD 與狀態機，加上去讓系統更完整，但不會增加它回答不了的問題。範疇邊界本身是一個工程決策，見 [ADR-0017](docs/adr/0017-scope-boundary-three-stage-mainline.md)。

## 快速開始

```bash
./mvnw spring-boot:run          # 後端（需先啟動 SQL Server）
cd frontend && npm run dev      # 前端
./mvnw test                     # 測試
```

詳細環境設定、Docker、資料初始化見 [docs/development.md](docs/development.md)。

## 工程決策

架構決策全數記錄在 [docs/adr/](docs/adr/)，包含被否決的替代方案與升級條件。幾個關鍵的：

- 為什麼用**傳統三層**而不是 DDD 分層
- 為什麼**不拆微服務**（配貨的單一交易邊界與庫存悲觀鎖是正確性來源，拆開要付 saga 的代價）
- 為什麼取號用**悲觀鎖**而非樂觀鎖或原子計數器
- 為什麼**手刻 JWT** 而不引入 Spring Security，以及該決策的已知風險與對策
- 為什麼回應**不包統一信封**

技術不預先引入：用得到才加，加了必須留下 repo 內看得到的證物（設定檔、壓測數據、決策紀錄）——「我用過」不算證物。

## 階段目標

每階段的完成定義是「可展示」，不是「寫完了」。

| 階段 | 目標 | 完成定義 |
|------|------|---------|
| **1. 補完主線** | SRO 領貨（AOD → SROD、大庫 → 車存）、`@RequireRole` 上到凍結／配貨端點 | 訂貨→配貨→領貨一條龍整合測試綠燈 |
| **2. 最小前端** | Branch 頁手寫，其餘代工；Element Plus 導入 | demo 主路徑可用滑鼠走完，401/409 體驗正確 |
| **3. 一鍵部署** | Docker multi-stage（前後端同 image）、non-root、healthcheck；Jenkins 前端 build stage | `docker-compose up` 一鍵可用 |
| **4. 驗證與觀測** | 併發整合測試（Testcontainers 真並發）、Redis INCR 版取號＋兩方案壓測、效能 baseline → 改 → 再測 | 帶數據的壓測報告；沒有 baseline 的優化視為沒做 |
| **5. 基礎設施** | k8s manifest／probe／多副本；每日庫存快照排程服務抽出（多副本下必須外移）；Linux 手動部署 runbook（做在雲 VM 上） | 「多副本之後什麼壞了」有具體修復紀錄 |

橫跨各階段的**基礎能力**（HTTP 全鏈路、交易隔離級別、`@Transactional` 失效、JVM 觀測、安全自評）不另立階段，掛在產生它的工作上，見 [ADR-0018](docs/adr/0018-fundamentals-via-mounted-evidence.md)。
