# 2. 採用傳統三層架構

日期：2026-06-29

## 狀態

已接受 (Accepted)

## 背景

本專案模擬經銷體系的倉儲配貨流程（訂貨 → 配貨 → 領貨出庫），目的是練習全端開發、累積對 Spring Boot 與企業系統慣例的理解。在組織後端程式時，需要選定一套架構風格。可選方案包括：

- **傳統三層架構**（controller / service / repository）：Spring 生態最主流、教學資源最多、團隊最熟悉。
- **六角形 / Clean Architecture**（ports & adapters、依賴反轉）：對領域邏輯隔離更好，但概念負擔重、樣板多。
- **領域驅動設計（DDD）含 rich domain model**：業務邏輯內聚於 entity，但對學習階段而言過度設計。

考量本專案是學習場域、以手動開發為主，重點在把主流慣例練熟，而非挑戰高階架構。

## 決策

我們將採用**傳統三層架構**，根 package `com.agony.wmsallocation`：

- 分層：`controller → service → repository → entity`，依賴單向，禁止反向依賴。
- **Entity 採 anemic model**（純資料類別，只有欄位 + Lombok getter/setter），所有業務邏輯一律寫在 Service。
- Entity 按業務領域分子目錄（`entity/{領域}/`），領域 enum 與 Entity 同目錄。

操作細節見 [docs/conventions/backend.md](../conventions/backend.md)。

## 後果

- 結構直觀、上手快，與絕大多數 Spring Boot 教材與既有專案慣例一致，符合學習目標。
- anemic model + service 集中邏輯的取捨：犧牲了領域物件的內聚性，換取分層清晰與易於理解；當業務邏輯變複雜時，service 可能膨脹，屆時可再評估抽取領域服務或局部引入 rich model。
- 明確的依賴方向讓相依關係容易推理，但需要靠紀律（與 review）維持，沒有引入編譯期的模組邊界強制工具。
