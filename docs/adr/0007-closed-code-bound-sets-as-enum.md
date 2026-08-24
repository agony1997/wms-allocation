# 7. 封閉且綁定程式行為的集合用 enum，不建資料表

日期：2026-07-01

## 狀態

已接受 (Accepted)

## 背景

設計 `entity/sequence/SequenceType`（單據類型：SPO/BPO/FDO/AO…共 12 種，每種對應一個單號前綴）時，浮現一個會反覆出現的問題：**這種「一組固定分類值」該建成 Java enum，還是存進資料表（附 lookup table）？**

本專案目前所有同類集合（`ActiveStatus`、`BpoStatus`、`FrozenStatus`、`LocationType`、`SequenceType` 等）都是 enum，直接以 `@Enumerated(EnumType.STRING)` 存字串到欄位，沒有另建 lookup table 配外鍵。但這個選擇之前從未明確決策過，只是每次個別判斷都恰好落在同一邊——沒有記錄下判斷準則本身，之後遇到新的集合（例如未來的 `AllocationStatus`、`ReturnReason`）容易重新猶豫、甚至不一致地選擇資料表。

以 `SequenceType` 為具體案例：它的每一個值都對應到 codebase 裡已存在的一個 entity/業務流程（SPO ↔ `SalesPurchaseOrder`、BPO ↔ `BranchPurchaseOrder`……）。新增一個值，必然伴隨新增 Entity/Service/Controller/業務邏輯——不可能只在資料表裡加一列就讓新單據類型生效。

## 決策

我們將以**「新增一個值，是否需要跟著寫新程式碼？」**作為判斷準則：

- **會**（值與程式行為緊密綁定，例如單據類型、狀態機的狀態）→ **Java enum**，直接以 `@Enumerated(EnumType.STRING)` 存字串欄位，不建 lookup table、不加外鍵。
- **不會**（業務／營運人員應該能在不改程式碼、不重新部署的情況下自行新增，例如商品分類、付款方式等會隨業務政策調整的清單）→ **資料表**。

`SequenceType` 屬前者，維持 enum，`document_sequence.sequence_type` 維持 `VARCHAR` 直存 `code`，不建 lookup table。

## 後果

- 集合值的新增/異動有編譯期檢查（型別安全），呼叫端寫錯值會在編譯期而非執行期被抓到。
- 若某個枚舉的顯示用中文名稱（如 `SPO("SPO", "業務員訂貨單")`）需要之後由營運人員自行修改文案，屆時應重新評估是否要拆出去——目前這類文案仍與程式碼綁定，維持在 enum 內。
- 與既有慣例（`ActiveStatus` 等）保持一致，不會出現「同類集合、不同建模方式」的不一致。
- 操作規則已收斂進 [docs/conventions/backend.md](../conventions/backend.md) 的 Entity 規範。
