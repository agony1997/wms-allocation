# 9. 業務員訂貨單改 lazy create，查詢不建立資料

日期：2026-07-02

## 狀態

已接受 (Accepted)

## 背景

原始規格（[SalesPurchase.md](../requirements/specification/purchase/SalesPurchase.md)）寫「查詢時若無訂單，系統自動建立該人訂單」。推測此行為源自舊系統的 UX 假設——**日配型業務員每天必下單**，故打開畫面就先備好單頭，省去一個「建立」動作。但這假設當初照原始需求沿用，我方系統設計未重審。

實作前重新檢視，「查即建」有一組**白付的代價**：

- **GET 帶副作用**：查詢本應冪等。此設計下重試／雙擊／前端重送／監控探測都在寫 DB；併發兩個 GET 同時查不到 → 都嘗試 insert → 撞唯一鍵 `(branchCode, locationCode, purchaseDate)` 爆錯，反而要為 GET 寫併發衝突處理。
- **取號被稀釋**：只要有人點開某天看一眼就 `generateSequence(SPO)` +1，序號不再等於真實訂單數；且取號有每日上限 999（[ADR-0008](0008-defer-sequence-cold-start-gap.md) 脈絡），空查也吃額度。
- **空單堆積**：畫面可選 `D+2 ~ D+9` 七天，業務員只是切過去瀏覽也會被順手建出空單。
- **語意被吃掉**：「這業務員今天到底下沒下單」再也不能用「SPO 是否存在」回答。

關鍵反問——**「業務員某天沒下單」是否為系統要能識別的狀態？** 答：是（雖目前無報表等消費端）。這一條即足以否決查即建，因為查即建讓「沒下單」永不存在。

面臨的選項：

- **A：維持查即建**（GET 無單就建空單）。
- **B：lazy create**（查詢唯讀，首次儲存才取號建單）。
- **C：查即建改 POST + 前端顯式「建立訂單」鈕。**

## 決策

我們將採 **B：lazy create**。

- `GET /api/sales-purchase-orders`（唯讀）：無單回傳空白表單（`purchaseNo=null, details=[], editable`），**不寫入任何資料**。
- `PUT /api/sales-purchase-orders`（upsert，業務鍵 = `branchCode + locationCode + purchaseDate` 置於 body）：無單則取號建立、有單則更新。首次儲存才是訂單真正誕生的時刻。
- **不為「漏單識別」預先蓋任何東西**（報表、漏單查詢 API）。B 讓「SPO 存在 = 已下單」自然成立，漏單識別能力免費附帶；將來真有消費端再查即可（YAGNI）。

## 後果

- **正面**：GET 恢復冪等、無副作用；取號＝真實下單數，不再被空查稀釋、不空吃 999 額度；不產生空單；語意乾淨（SPO 存在＝已下單，漏單天然可識別）；消除併發 GET 撞唯一鍵的問題。決策發生在實作前，只有 entity + 骨架，**零沉沒成本**。
- **負面／須承擔**：儲存端點變 upsert，前端要處理「儲存前尚無 `purchaseNo`」的狀態。`save` 內「無單 → 建」仍有併發插入撞唯一鍵的可能，但這是**真正的下單動作**、頻率遠低於查詢，且有唯一鍵兜底；屆時比照 [ADR-0005](0005-testing-strategy-by-uncertainty.md) 由 test-first 併發測試驅動修補。
- **被否決的 A**：其副作用是白付成本，而它唯一的好處（UX 少按一次「建立」）B 也有——lazy create 體感相同（打開即可填），只是「建」挪到存檔那一刻。故否決。
- **被否決的 C**：多一步顯式建立，違背「想少一步」的初衷，多此一舉。故否決。
- 未取代任何既有 ADR（本案修訂的是訂貨規格行為，非推翻架構決策）；與 [ADR-0003](0003-restful-response-without-envelope.md) 一致（PUT upsert 回資源本體）。規格已於 [SalesPurchase.md](../requirements/specification/purchase/SalesPurchase.md) 同步更新。
