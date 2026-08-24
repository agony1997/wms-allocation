# 13. 庫存併發控制採悲觀鎖

日期：2026-07-13

## 狀態

已接受 (Accepted)

## 背景

`Inventory` 的所有增減都是「查 → 記憶體加減 → save」的 read-modify-write，無 `@Version`、無 `@Lock`（如 `InventoryService.allocate()`）。跨交易並發扣同一批次會 lost update：兩筆交易各自讀到相同餘額、各自減、後寫覆蓋前寫，最終超扣。單人 demo 踩不到，但配貨（多業務員同時搶同一批大庫）正是這類競爭的入口，且 ADR-0008 規劃的併發整合測試遲早要面對。

配貨動工的前置：規格定案 A1–A5、庫存例外契約與測試（S1+S2）皆已完成，唯獨鎖策略留待此處以 ADR 定案。候選方案：

- **`@Version` 樂觀鎖**：改動最小，但衝突時拋例外需要重試邏輯，擴散到每個異動方法。
- **`PESSIMISTIC_WRITE` 悲觀鎖**：與 ADR-0006 取號同一款心智模型，一致性好，鎖粒度較大。
- **原子 UPDATE**（`update ... set qty = qty - :n where ... and qty >= :n`）：最省且天然擋超扣，但繞過 JPA 一級快取，與其他走 entity 的操作混用時要小心。

## 決策

我們將對庫存併發控制採**悲觀鎖**，與 [ADR-0006](0006-sequence-single-pessimistic-lock.md) 同一手法：

- 在 `InventoryRepo` 新增一個帶 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 的精確查詢方法，對目標 `Inventory` 列序列化存取（交給 Hibernate 轉成 SQL Server 對應的鎖提示，不手刻 `FOR UPDATE`）。
- **扣庫類操作統一走它**：`InventoryService` 的私有共用方法 `getInventoryOrThrow()` 改用這個 locked finder，一處改動即讓所有經過它的扣庫方法（allocate / sell / keep / keepRetrieve / returnGoods / returnShip）同時受鎖保護——這些路徑的目標列必已存在（扣庫的前提是有貨），行鎖完全有效。
- **不使用樂觀鎖、不使用原子 UPDATE**：不引入 `@Version` 及其重試；維持統一走 entity 的寫法，避免快取一致性的心智負擔。
- 落地分階段：本次配貨先讓 `allocate` 路徑生效（其餘扣庫方法因共用 `getInventoryOrThrow` 一併到位）；領貨 `pickUp` 屬入庫類，見下方缺口。

一致性優先於極致效能：配貨本來就是低頻批次操作，悲觀鎖較大的鎖粒度在此情境成本可接受，換得與既有取號相同的心智模型（學習專案的一致性價值高於效能）。

## 後果

- 設計與程式意圖清楚，且與 ADR-0006 共用同一套併發思路，維護者只需理解一種鎖。
- **已知缺口（實作須知）**：入庫類操作（receive / pickUp / customerReturn）走 `findOrCreateInventory`，目標列當日首筆時尚不存在，`PESSIMISTIC_WRITE` 鎖不到不存在的列 → 並發首筆可能同時 insert 而撞 `Inventory` 唯一鍵。這是行鎖蓋不到「建列」的經典缺口（同 ADR-0006）。入庫是加法、且有唯一約束保底，超扣風險集中在扣庫類（列必存在，已受鎖保護）；建列競爭以「捕捉插入衝突後重讀該列再累加」處理，屬需求驅動的後續強化。
- 此類缺口單元測試（看紅）抓不到，只有多執行緒整合測試會現形——呼應 [ADR-0005](0005-testing-strategy-by-uncertainty.md)：併發那層走 Testcontainers 整合測試。
- 單節點 SQL Server 下悲觀鎖已足夠；分散式需求屬需求驅動的未來升級，現在不做。
- **待辦（程式碼）**：`InventoryRepo` 加 locked finder；`getInventoryOrThrow()` 改走它。
