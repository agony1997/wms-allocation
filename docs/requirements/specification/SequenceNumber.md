取號機制規格書
===

> 設計系統內所有表格共用的取號方式

---

## 概述

系統中多種單據需要產生唯一的流水號，為確保併發安全與一致性，採用統一的取號機制。

---

## 需取號的單據類型

| 類型代碼 | 單據名稱 | 編號前綴 | 編號格式 |
|---------|---------|---------|---------|
| SPO | 業務員訂貨單 | SPO | SPO-{yyyyMMdd}-{3位序號} |
| BPF | 營業所凍結單 | BPF | BPF-{yyyyMMdd}-{3位序號} |
| BPO | 營業所訂貨單 | BPO | BPO-{yyyyMMdd}-{3位序號} |
| FDO | 工廠出貨單 | FDO | FDO-{yyyyMMdd}-{3位序號} |
| AO | 配貨單 | AO | AO-{yyyyMMdd}-{3位序號} |
| SRO | 業務領貨單 | SRO | SRO-{yyyyMMdd}-{3位序號} |
| CPO | 客戶預訂單 | CPO | CPO-{yyyyMMdd}-{3位序號} |
| SDO | 送貨單 | SDO | SDO-{yyyyMMdd}-{3位序號} |
| AR | 應收帳款 | AR | AR-{yyyyMMdd}-{3位序號} |
| SKR | 業務員寄庫單 | SKR | SKR-{yyyyMMdd}-{3位序號} |
| SRR | 業務員退庫單 | SRR | SRR-{yyyyMMdd}-{3位序號} |
| BRO | 營業所銷退單 | BRO | BRO-{yyyyMMdd}-{3位序號} |

> 類型清單與 `entity/sequence/enums/SequenceType.java` 同步，新增單據類型時兩邊需一併更新。

---

## 取號表設計

### 表格結構

```
document_sequence (單據序號表)
├── sequence_type   : VARCHAR(10)  -- 類型代碼 (PK)
├── sequence_date   : DATE         -- 日期 (PK)
└── current_no      : INT          -- 當前序號
```

### 複合主鍵

- `sequence_type` + `sequence_date` 組成複合主鍵
- 每種單據類型每天獨立計號
- 序號每日重置從 1 開始

---

## 併發控制機制

> 只用悲觀鎖、不再疊加樂觀鎖，決策與理由見 [ADR-0006](../../adr/0006-sequence-single-pessimistic-lock.md)。

### 單一悲觀鎖策略

```
┌─────────────────────────────────────────────────────┐
│                 取號流程                             │
├─────────────────────────────────────────────────────┤
│  1. @Transactional(REQUIRES_NEW)  ← 獨立事務        │
│     即時提交、縮短持鎖，避免外層回滾吸回已發號        │
│                                                     │
│  2. @Lock(PESSIMISTIC_WRITE)      ← 悲觀鎖 (行級)   │
│     鎖定該類型+日期的記錄，序列化存取                │
│     （Hibernate 於 SQL Server 轉為對應鎖提示）      │
│                                                     │
│  3. current_no + 1                                  │
│     遞增序號                                        │
└─────────────────────────────────────────────────────┘
```

悲觀鎖的行鎖已將同一 `(sequenceType, sequenceDate)` 的取號序列化，足以保證不重號，故不再使用樂觀鎖（`@Version`）與其重試。

### 為何使用獨立事務？

| 情境 | 無獨立事務 | 有獨立事務 |
|-----|-----------|-----------|
| 外層事務成功 | 序號正常遞增 | 序號正常遞增 |
| 外層事務回滾 | 序號一併回滾，產生重複 | 序號已提交，不會重複 |

> 獨立事務與鎖的選擇正交：前者管交易邊界，後者管並發互斥。

### 冷啟動缺口（實作須處理）

某 `(type, date)` 當日首筆取號時該列尚不存在，行鎖鎖不到不存在的列 → 並發首筆可能同時 insert 而撞複合主鍵。可採「預先建列／捕捉插入衝突後重讀該列再取號／upsert」處理。此缺口需整合＋多執行緒測試才驗證得到。

### 各資料庫的鎖轉譯差異與規模關聯

`@Lock(PESSIMISTIC_WRITE)` 本身不產生鎖，只是告訴 JPA provider「我要排他鎖」；實際轉譯成什麼 SQL，由 Hibernate 的 Dialect 決定，因資料庫而異：

| DB | 轉譯 | 特有風險 |
|----|------|---------|
| **SQL Server**（本專案） | `WITH (UPDLOCK, ROWLOCK, HOLDLOCK)` 等 table hint | 鎖升級（lock escalation）：單一陳述式/交易鎖太多列時可能自動升級成頁鎖/表鎖；預設 `LOCK_TIMEOUT = -1`（無限等待），沒設定會一路卡住 |
| MySQL / InnoDB | `SELECT ... FOR UPDATE` | 預設 `REPEATABLE READ` 下的 gap lock（間隙鎖）：條件沒精準打中唯一索引時，鎖的範圍比預期大，可能造成不相關查詢被阻塞甚至死鎖 |
| PostgreSQL | `SELECT ... FOR UPDATE` | 相對單純，但 `lock_timeout` 預設關閉，一樣可能無限等待 |

**跟並發使用者數的關聯**：使用者數越多，同一秒內打同一個 `(sequenceType, sequenceDate)` 取號的機率越高，會放大兩個既有風險：

1. 排隊等鎖的請求變多；沒設定逾時，等待鏈可能拖得很長（尤其 SQL Server 預設無限等待）。
2. **冷啟動缺口**被觸發的機率也隨並發數上升——當日某類型「第一次」取號的瞬間，正是唯一沒被行鎖保護的時刻，使用者一多，這個瞬間被兩個以上請求同時撞到的機率隨並發數上升得比線性更快。

**若未來更換資料庫**（見下方「分散式環境」）：`@Lock(PESSIMISTIC_WRITE)` 這行程式碼不用改，但底下實際產生的 SQL 與邊界情況（尤其 gap lock）不是同一回事，必須針對新方言重新做整合測試，不能假設「程式碼一樣＝行為一樣」。

---

## 編號格式規則

### 格式組成

```
{前綴}-{日期}-{序號}
  │      │      │
  │      │      └── 3位數字，不足補零 (001-999)
  │      └────────── yyyyMMdd 格式
  └───────────────── 單據類型前綴
```

### 範例

| 類型 | 日期 | 序號 | 完整編號 |
|-----|------|-----|---------|
| SPO | 2024-01-22 | 1 | SPO-20240122-001 |
| SPO | 2024-01-22 | 15 | SPO-20240122-015 |
| BPO | 2024-01-22 | 1 | BPO-20240122-001 |

---

## API 設計

### 取號服務

目前只有悲觀鎖一種實作，暫不抽介面（YAGNI）；程式碼中以 `// ponytail: 加其他實作時再抽介面` 註明觸發條件，未來若要新增樂觀鎖／Redis 等替代實作，再抽出介面。

```java
@Service
@RequiredArgsConstructor
public class SequenceService {

    private final SequenceRepo sequenceRepo;

    /**
     * 產生單據編號
     * @param sequenceType 單據類型
     * @param sequenceDate 單據日期
     * @return 完整單據編號
     */
    public String generateSequence(SequenceType sequenceType, LocalDate sequenceDate) {
        // ...
    }
}
```

### 單據類型列舉

```java
public enum SequenceType {
    // SalesPurchaseOrder
    SPO("SPO", "業務員訂貨單"),
    // BranchPurchaseFrozen
    BPF("BPF", "營業所凍結單"),
    // BranchPurchaseOrder
    BPO("BPO", "營業所訂貨單"),
    // FactoryDeliveryOrder
    FDO("FDO", "工廠出貨單"),
    // AllocationOrder
    AO("AO", "配貨單"),
    // SalesReceiveOrder
    SRO("SRO", "業務領貨單"),
    // CustomerPreOrder
    CPO("CPO", "客戶預訂單"),
    // SalesDeliveryOrder
    SDO("SDO", "送貨單"),
    // AccountReceivable
    AR("AR", "應收帳款"),
    // SalesKeepRecord
    SKR("SKR", "業務員寄庫單"),
    // SalesReturnRecord
    SRR("SRR", "業務員退庫單"),
    // BranchReturnOrder
    BRO("BRO", "營業所銷退單");

    private final String code;
    private final String name;
}
```

> 以上與 `entity/sequence/enums/SequenceType.java` 實際程式碼一致。

---

## 使用方式

### 在 Service 中注入使用

```java
@Service
@RequiredArgsConstructor
public class SalesPurchaseOrderService {

    private final SequenceService sequenceService;

    public SalesPurchaseOrder createOrder(...) {
        String orderNo = sequenceService.generateSequence(
            SequenceType.SPO,
            purchaseDate
        );
        // ...
    }
}
```

---

## 異常處理

| 異常情況 | 處理方式 |
|---------|---------|
| 序號超過 999 | 拋出 BusinessException |
| 並發取號 | 悲觀鎖序列化，後到者等待前者 commit 後再讀取遞增 |
| 當日首筆並發插入 | 複合主鍵 (type, date) 擋重複；捕捉插入衝突後重讀該列再取號 |

---

## 擴展考量

### 序號位數不足

當單日序號可能超過 999 時：
1. 調整格式為 4 位數：`{前綴}-{yyyyMMdd}-{4位序號}`
2. 或改用時間戳：`{前綴}-{yyyyMMddHHmmss}-{3位序號}`

### 分散式環境

若未來需要分散式部署：
1. 可改用 Redis 的 INCR 命令
2. 或使用 Snowflake 演算法
3. 目前的悲觀鎖方案在單一資料庫下已足夠

---

## 相關表格

- [Table.md](./Table.md) - 系統表格總覽
- [SalesPurchase.md](./purchase/SalesPurchase.md) - 業務員訂貨規格
