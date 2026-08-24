# Architecture Decision Records (ADR)

本目錄記錄專案的**架構決策**：每個重要決策一篇 markdown，說明「在什麼背景下、為什麼這樣決定、帶來什麼後果」。
ADR 是決策的歷史紀錄，**只增不改**——決策被推翻時不刪舊檔，而是新增一篇並把舊篇標記為「已取代 (Superseded)」。

> ADR 記錄**為什麼**這樣決定；操作層面的**怎麼做**寫在 `docs/conventions/`。兩者互相連結，不重複。

## 慣例

- 檔名：`NNNN-kebab-title.md`，`NNNN` 為四位流水號（0001、0002…）
- 格式：採 [Michael Nygard 格式](template.md)（背景 / 決策 / 後果）
- 狀態：`提議中 (Proposed)` / `已接受 (Accepted)` / `已棄用 (Deprecated)` / `已取代 (Superseded by ADR-XXXX)`
- 新增方式：複製 [template.md](template.md)，取下一個流水號，寫完後在下方索引補一列

> **編號有缺口是正常的**（0004、0012、0014、0015、0016）。本專案的技術決策承襲自前身專案，那些編號是已失效的定位／規劃類決策，隨專案範疇重訂而作廢，未帶過來。

## 索引

| 編號 | 標題 | 狀態 |
|------|------|------|
| [0001](0001-record-architecture-decisions.md) | 採用 ADR 記錄架構決策 | 已接受 |
| [0002](0002-adopt-traditional-three-layer-architecture.md) | 採用傳統三層架構 | 已接受 |
| [0003](0003-restful-response-without-envelope.md) | RESTful 回應不使用統一信封 | 已接受 |
| [0005](0005-testing-strategy-by-uncertainty.md) | 測試策略：依不確定性選 test-first／test-after | 已接受 |
| [0006](0006-sequence-single-pessimistic-lock.md) | 取號併發控制採單一悲觀鎖 | 已接受 |
| [0007](0007-closed-code-bound-sets-as-enum.md) | 封閉且綁定程式行為的集合用 enum，不建資料表 | 已接受 |
| [0008](0008-defer-sequence-cold-start-gap.md) | 取號冷啟動缺口暫不處理，延到併發整合測試 | 已接受 |
| [0009](0009-lazy-create-sales-purchase-order.md) | 業務員訂貨單改 lazy create，查詢不建立資料 | 已接受 |
| [0010](0010-custom-jwt-auth-without-spring-security.md) | 採用手刻 JWT 驗證，不引入 Spring Security | 已接受 |
| [0011](0011-externalize-secrets-to-env-vars.md) | 祕密改由環境變數注入，不寫進版控 | 已接受 |
| [0013](0013-inventory-pessimistic-lock.md) | 庫存併發控制採悲觀鎖 | 已接受 |
| [0017](0017-scope-boundary-three-stage-mainline.md) | 範疇界定為訂貨→配貨→領貨出庫三段主線 | 已接受 |
| [0018](0018-fundamentals-via-mounted-evidence.md) | 補強網路協議與執行期原理，證物採掛載式 | 已接受 |
