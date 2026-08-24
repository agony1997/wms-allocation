# 6. 取號併發控制採單一悲觀鎖

日期：2026-07-01

## 狀態

已接受 (Accepted)

## 背景

系統多種單據需產生唯一流水號（格式 `{前綴}-{yyyyMMdd}-{3位序號}`，每類型每日獨立計號），取號必須併發安全。

`SequenceNumber.md` 原設計要求「雙重鎖定」：`REQUIRES_NEW` 獨立事務 + `SELECT ... FOR UPDATE` 悲觀鎖 + `@Version` 樂觀鎖 + 重試×3；`DocumentSequence` entity 也帶了 `@Version` 欄位。

檢視後認定此設計對取號情境是過度設計：

- **悲觀鎖與樂觀鎖是解同一問題（並發改同一列）的兩種替代手段，非互補**。一旦持有 `FOR UPDATE` 的行鎖，該列在本交易 commit 前無法被他人修改，等到 UPDATE 時 `@Version` 檢查永遠不會失敗——樂觀鎖在此路徑上是死碼。spec 自己也把 `@Version` 標為「額外保護」，正是 belt-and-suspenders 的自白。
- **對「演練併發」而言，同時用反而自我矛盾**：樂觀鎖的重試只在版本衝突時觸發，但悲觀鎖已擋掉所有衝突，那段重試永遠跑不到，根本無從演練。要練樂觀鎖，就不能同時上悲觀鎖。
- 取號屬「臨界區短、競爭高」的計數器情境，**悲觀鎖是標準解法**。

## 決策

我們將對取號的併發控制採**單一悲觀鎖**：

- 在 repository 的查詢方法掛 `@Lock(LockModeType.PESSIMISTIC_WRITE)`，對 `(sequenceType, sequenceDate)` 該列序列化存取（Hibernate 於 SQL Server 轉為對應的鎖提示，不手刻 `FOR UPDATE`）。
- **保留 `REQUIRES_NEW` 獨立事務**——這是交易邊界問題（讓取號即時提交、避免呼叫端回滾把已發號吸回，並縮短持鎖時間），與「選哪種鎖」正交，兩件事分開看。
- **不使用樂觀鎖**：不做 `@Version` 相關的重試；`DocumentSequence` entity 上的 `@Version` 欄位一併移除。
- 若日後要演練樂觀鎖，另以獨立實驗（樂觀鎖-only + 重試）進行，**不與悲觀鎖並用**。

本決策同時修訂 [SequenceNumber.md](../requirements/specification/SequenceNumber.md) 使其一致。

## 後果

- 設計與程式更單純、意圖清楚，符合計數器情境的標準做法。
- **已知缺口（實作須處理）**：某 `(type, date)` 當日首筆取號時該列尚不存在，`FOR UPDATE` 鎖不到不存在的列 → 並發首筆可能同時 insert 而撞複合主鍵。這是行鎖蓋不到「建列」的經典缺口，須以「預先建列／捕捉插入衝突後重讀該列再取號／upsert」等方式處理。此類缺口單元測試（看紅）抓不到，只有整合＋多執行緒測試會現形——呼應 [ADR-0005](0005-testing-strategy-by-uncertainty.md)：併發那層走 Testcontainers 整合測試。
- **待辦（程式碼）**：移除 `DocumentSequence` 的 `@Version` 欄位，並修正其 Javadoc 中「悲觀鎖搭配樂觀鎖」的敘述。
- 單節點 SQL Server 下悲觀鎖已足夠；分散式需求（Redis `INCR` / Snowflake）屬需求驅動的未來升級，現在不做。
