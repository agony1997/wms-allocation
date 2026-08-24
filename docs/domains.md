# 業務領域對照

`entity/{領域}/` 子目錄與業務的對應關係。業務規格位於 `docs/requirements/specification/`（按領域分子目錄）。

| 子目錄 | 業務 |
|--------|------|
| auth | 認證授權 |
| branch | 營業所 |
| purchase | 訂貨 |
| receive | 收貨 |
| inventory | 庫存 |
| master | 主檔（商品/客戶/工廠/銷售組織） |
| sequence | 單號序號產生 |
| allocation | 配貨、業務員領貨 |

> **暫緩**：配送（delivery）與結帳退貨（closing）不在當前實作範疇，entity 子目錄未建立；規格仍保留在 `docs/requirements/specification/delivery/`、`closing/`。

## 業務流程

```
訂貨 (Purchase) → 收貨 (Receive) → 配貨 (Allocation) → 領貨出庫 (SRO)
                                            │
                          主檔/營業所設定 (Master/Branch)

暫緩：送貨 (Delivery) → 結帳 (Closing)
```
