# 8. 取號冷啟動缺口暫不處理，延到併發整合測試

日期：2026-07-02

## 狀態

已接受 (Accepted)

## 背景

[ADR-0006](0006-sequence-single-pessimistic-lock.md) 把取號改採單一悲觀鎖後，留下一個**已知缺口**：某 `(sequenceType, sequenceDate)` 當日首筆取號時該列尚不存在，行鎖鎖不到不存在的列 → 並發首筆可能同時 insert 而撞複合主鍵。本 ADR 決定「這個缺口現在要不要補防護」。

補充一個 ADR-0006 未展開、會影響決策的事實 —— **SQL Server 上這個缺口比「純行鎖」的心智模型更小**：

- `@Lock(PESSIMISTIC_WRITE)` 在 SQL Server 方言被翻成 `WITH (UPDLOCK, ROWLOCK, HOLDLOCK)`，其中 `HOLDLOCK` 等於把該 statement 拉到 SERIALIZABLE。
- 查詢 `WHERE sequence_type=? AND sequence_date=?` 打在複合主鍵索引（index seek）。SERIALIZABLE + 索引 seek 打到不存在的鍵時，SQL Server 不是「沒鎖」，而是鎖住「那把鍵應被插入的索引間隙」（key-range lock）。
- 配 `UPDLOCK`（U 鎖互斥），乾淨情境下兩個併發首筆會被序列化：後到者卡在 range 鎖上，等前者 insert+commit 後醒來，讀到已存在的列 → 改走 UPDATE 遞增，而非再 insert。

也就是說，SQL Server 的實際鎖行為多半已把首筆序列化，缺口不太容易踩到。但這份保護是**有前提且不可攜**的：依賴 index seek + SERIALIZABLE、跨 DB 語意不同（InnoDB gap lock、Postgres `FOR UPDATE` 各異）、高併發下 range 鎖放大死鎖面、換 DB 就得重評。

面臨的兩個選項：

- **A：暫不處理，延到寫併發整合測試時再補。**
- **B：現在就做預建列**（當日首筆前預先 insert `current_no=0` 的列）。

## 決策

我們將採 **A：暫不處理，把修補延到多執行緒整合測試**。並確立：

- **觸發點明確**：不是「有空再說」，而是「當開始寫多執行緒併發整合測試時」。那正是唯一能重現此缺口、也唯一能證明防護有效的時機——呼應 [ADR-0005](0005-testing-strategy-by-uncertainty.md)：併發那層走 Testcontainers 整合測試，test-first（先紅）再補防護。
- **屆時採「捕 PK 衝突 → 重讀該列 → 遞增」，而非預建列**。預建列需要一支排程 job 並列舉「未來每一天 × 每種類型」的組合，或退化成 lazy「INSERT IF NOT EXISTS」把同一個併發問題搬到 insert 那步——機具重、且沒真的解掉。捕衝突重讀是可攜、不需排程、不需列舉日期的最小修法。
- **以程式碼註解顯性追蹤**：在 `SequenceService` 首筆建列（`orElseGet`）處加 `// ponytail:` 標記，寫明天花板與升級路徑並指回本 ADR，讓延後從口頭決策變成掃得到的 debt，不落入「later 等於 never」。

現在不動任何併發防護程式碼。

## 後果

- **正面**：不新增現在驗證不了的碼；符合 ADR-0005「不確定性高才 test-first」——併發正確性屬高不確定性，防護該由整合測試驅動出來，而非憑空先寫。學習專案單人操作下此缺口幾乎踩不到，延後的實務風險很低。
- **負面／須承擔**：缺口在帳面上仍開著。若**換資料庫**或**併發量顯著上升**，SQL Server key-range lock 的隱性保護可能失效或提前現形，須重新評估並提前執行修補——不能假設「現在沒事＝永遠沒事」。
- **被否決的 B（預建列）**：否決理由如上（需排程 + 列舉日期，或 lazy 版沒解掉問題）；記錄在案，避免未來又把它當成「乾淨的首選」重議。
- 本決策不修改程式邏輯，僅新增一則 `ponytail:` 註解；缺口說明已在 [SequenceNumber.md](../requirements/specification/SequenceNumber.md)「冷啟動缺口」一節。
