表格
===
---
不進SAP，嘗試用別的方式模擬，簡化相關表格。

---

## 組織與權限

> - 銷售組織 (SalesOrganization)
> - 營業所 (Branch)
> - 儲位 (Location)
> - 使用者 (AuthUser)
> - 角色 (AuthRole)
> - 代理人設定 (SalesSubstitution)
> - 使用者營業所角色 (AuthUserBranchRole)
>
> 詳見 [Branch.md](./master/Branch.md)、[Location.md](./master/Location.md)、[User.md](./master/User.md)

### 組織結構

```
SalesOrganization (銷售組織)
└── Branch (營業所)
    ├── Location (儲位)
    │   ├── WAREHOUSE (大庫, locationCode = branchCode)
    │   └── CAR (業務員車存)
    │
    ├── SalesSubstitution (代理人設定)
    │
    └── AuthUserBranchRole (使用者在此營業所的角色)
        ├── AuthUser (使用者)
        └── AuthRole (角色)
```

### 多營業所角色範例

```
User U001
├── 營業所 1000: LEADER + SALES (組長兼業務員)
└── 營業所 2000: SALES (業務員)
```

### 角色

| 角色代碼 | 名稱 | 說明 |
|----------|------|------|
| SALES | 業務員 | 訂貨、領貨、送貨、寄庫、退庫 |
| LEADER | 組長 | 凍結、調整確認數量 |
| WAREHOUSE | 庫務 | 收貨、配貨、彙整銷退 |
| ADMIN | 系統管理員 | 全部權限 |

### 儲位與業務員

```
User (1) ─────< Location (N)
一個業務員可有多儲位（不同營業所）
```

---

## 訂貨作業

> - 業務員訂貨單 (SalesPurchaseOrder / SPO)
> - 業務員訂貨單明細 (SalesPurchaseOrderDetail / SPOD)
> - 業務員自訂產品清單 (SalesPurchaseList)
> - 營業所凍結單 (BranchPurchaseFrozen / BPF)
> - 營業所訂貨單 (BranchPurchaseOrder / BPO)
> - 營業所訂貨單明細 (BranchPurchaseOrderDetail / BPOD)
>
> 詳見 [SalesPurchase.md](./purchase/SalesPurchase.md)、[BranchPurchase.md](./purchase/BranchPurchase.md)

### 訂貨表格關係

```
SPO (業務員訂貨單)
 │
 ├── SPOD (明細) ──► 彙總 ──► BPOD (營業所訂貨單明細)
 │                              │
 │                              └── BPO (營業所訂貨單，按工廠分)
 │
 └── 受 BPF (凍結單) 控制編輯權限
```

### 狀態設計考量

| 表格 | 狀態 | 說明 |
|------|------|------|
| SPO | 無 | 狀態由 BPF 和 SPOD 控制 |
| SPOD | PENDING / AGGREGATED / ALLOCATED | 待彙總 → 已彙總進 BPO → 已配貨（終態，分到 0 也標） |
| BPF | FROZEN / CONFIRMED | 控制編輯權限 |
| BPO | PENDING / RECEIVED / DISCREPANCY | 待收貨；收貨確認時依實收相符與否轉態（以 `BpoStatus` 為準） |

---

## 收貨

> - 工廠出貨單 (FactoryDeliveryOrder / FDO)
> - 工廠出貨單明細 (FactoryDeliveryOrderDetail)
>
> 詳見 [FactoryDeliveryOrder.md](./receive/FactoryDeliveryOrder.md)

---

## 配貨/領貨

> - 業務員優先度 (SalesPriority)
> - 配貨單 (AllocationOrder / AO)
> - 配貨單明細 (AllocationOrderDetail / AOD)
> - 業務領貨單 (SalesReceiveOrder / SRO)
> - 業務領貨單明細 (SalesReceiveOrderDetail / SROD)
>
> 詳見 [AllocationOrder.md](./allocation/AllocationOrder.md)、[SalesReceiveOrder.md](./allocation/SalesReceiveOrder.md)

### 配貨/領貨表格關係

```
SPOD (業務員訂貨明細)
 │
 ├── 庫務配貨 ──► AO (配貨單)
 │                 │
 │                 └── AOD (配貨明細，含批次、效期)
 │                       │
 │                       └── 業務員領貨 ──► SRO (領貨單)
 │                                           │
 │                                           └── SROD (關聯 AOD)
 │
 └── SalesPriority (業務員優先度，影響批次分配)
```

### 狀態設計

| 表格 | 狀態 | 說明 |
|------|------|------|
| AO | PENDING / RECEIVED | 追蹤整張配貨單狀態（全部明細皆領取完畢才轉為 RECEIVED） |
| AOD | PENDING / RECEIVED | 追蹤是否已被領取 |
| SRO | PENDING / RECEIVED | 追蹤領貨單狀態 |

---

## 送貨

> - 客戶預訂單 (CustomerPreOrder / CPO)
> - 客戶預訂單明細 (CustomerPreOrderDetail / CPOD)
> - 送貨單 (SalesDeliveryOrder / SDO)
> - 送貨單明細 (SalesDeliveryOrderDetail / SDOD)
> - 應收帳款 (AccountReceivable / AR)
>
> 詳見 [CustomerPreOrder.md](./delivery/CustomerPreOrder.md)、[SalesDeliveryOrder.md](./delivery/SalesDeliveryOrder.md)

### 送貨表格關係

```
CPO (客戶預訂)
 │
 ├── 業務員參考 ──► SPO (業務員訂貨)
 │
 └── 領貨完成後 ──► SDO (送貨單)
                    │
                    ├── SDOD (SALES) ─ 銷售
                    ├── SDOD (RETURN) ─ 退貨
                    └── SDOD (DAMAGE) ─ 毀損
                    │
                    └── 簽收完成 ──► AR (應收帳款)
```

### 狀態設計

| 表格 | 狀態 | 說明 |
|------|------|------|
| CPO | PENDING / COMPLETED / CANCELLED | 追蹤預訂單狀態 |
| SDO | PENDING / DELIVERED / SIGNED | 追蹤送貨單狀態 |
| SDOD | SALES / RETURN / RETURN_STALE / EXCHANGE / DAMAGE | 明細類型 |
| AR | PENDING / PARTIAL / PAID / OVERDUE | 追蹤收款狀態 |

---

## 營業所設定

> - 營業所產品排序 (BranchProductList)
> - 業務員優先度 (SalesPriority)
>
> 詳見 [BranchProductList.md](./branch/BranchProductList.md)、[SalesPriority.md](./branch/SalesPriority.md)

---

## 主檔表格

> - 產品 (Product)
> - 產品工廠對應 (ProductFactory)
> - 產品單位轉換 (ProductUnitConversion)
> - 工廠 (Factory)
> - 客戶 (Customer) (歸屬由 salesOrgCode + userCode 決定)
> - 銷售組織 (SalesOrganization)
>
> 詳見 [Product.md](./master/Product.md)、[Factory.md](./master/Factory.md)、[Customer.md](./master/Customer.md)、[SalesOrganization.md](./master/SalesOrganization.md)

### 產品工廠對應

```
Product ──< ProductFactory >── Factory
```

---

## 庫存表格

> - 庫存 (Inventory)
> - 庫存異動記錄 (InventoryTransaction)
> - 每日庫存快照 (InventoryDailySnapshot)
>
> 詳見 [Inventory.md](./master/Inventory.md)

### 庫存結構

```
Inventory = f(branchCode, locationCode, productCode, batchNo)
├── locationType: WAREHOUSE / CAR
├── qty: 可用數量
├── keepQty: 寄庫數量 (僅 WAREHOUSE)
└── returnQty: 待退庫數量 (僅 WAREHOUSE)

InventoryTransaction = 每筆庫存異動的流水帳
├── transactionType: 異動類型 (RECEIVE / ALLOCATE / PICK_UP / ...)
├── qtyChange / keepQtyChange / returnQtyChange: 各欄位變化量 (+/-)
└── sourceDocType + sourceDocNo: 來源單據

InventoryDailySnapshot = 每日結算的庫存快照（供報表用）
├── snapshotDate: 快照日期
└── 其餘欄位同 Inventory
```

### 儲位類型

| 類型 | 說明 | locationCode |
|------|------|--------------| 
| WAREHOUSE | 大庫 | = branchCode |
| CAR | 業務員車存 | 業務員儲位代碼 |

### 設計考量

- 採用餘額表 + 異動記錄 + 每日快照三表設計
- 餘額表查詢快，異動記錄可追溯，每日快照供報表
- 每筆異動有對應單據可追蹤
- 支援 FEFO（近效期先出，排序鍵為 `expiryDate`；用詞說明見 [Inventory.md](./master/Inventory.md)）


---

## 結束流程表格

> - 業務員寄庫單 (SalesKeepRecord / SKR)
> - 業務員寄庫單明細 (SalesKeepRecordDetail / SKRD)
> - 業務員退庫單 (SalesReturnRecord / SRR)
> - 業務員退庫單明細 (SalesReturnRecordDetail / SRRD)
> - 營業所銷退單 (BranchReturnOrder / BRO)
> - 營業所銷退單明細 (BranchReturnOrderDetail / BROD)
>
> 詳見 [SalesKeepRecord.md](./closing/SalesKeepRecord.md)、[SalesReturnRecord.md](./closing/SalesReturnRecord.md)、[BranchReturnOrder.md](./closing/BranchReturnOrder.md)

### 結束流程表格關係

```
業務員下班
├── SKR (寄庫) ──► 隔天併入 SRO 領回
│
└── SRR (退庫) ──► 庫務彙整 ──► BRO (按工廠分) ──► 工廠銷毀
```

### 狀態設計

| 表格 | 狀態 | 說明 |
|------|------|------|
| SKR | KEPT / RETRIEVED | 追蹤寄庫/領回狀態 |
| SRR | PENDING / PROCESSED | 追蹤是否已併入 BRO |
| BRO | PENDING / RETURNED | 追蹤是否已退回工廠 |

### 退庫原因

| 原因 | 說明 |
|------|------|
| STALE | 呆品（含過期、即期、賣不動） |
| DAMAGED | 損壞 |
| RECALLED | 召回 |

---

## 取號表格

> 詳見 [SequenceNumber.md](./SequenceNumber.md)

> - 單據序號表 (DocumentSequence)

### 單據類型

| 代碼 | 名稱 | 說明 |
|------|------|------|
| SPO | 業務員訂貨單 | 業務員向營業所訂貨 |
| BPF | 營業所凍結單 | 凍結營業所訂單 |
| BPO | 營業所訂貨單 | 營業所向工廠訂貨 |
| FDO | 工廠出貨單 | 工廠出貨給營業所 |
| AO | 配貨單 | 庫務配貨給業務員 |
| SRO | 業務領貨單 | 業務員領取貨品 |
| CPO | 客戶預訂單 | 客戶預訂（D+2 送達） |
| SDO | 送貨單 | 業務員送貨給客戶 |
| AR | 應收帳款 | 客戶應收款項 |
| SKR | 業務員寄庫單 | 業務員寄存貨品 |
| SRR | 業務員退庫單 | 業務員退還貨品 |
| BRO | 營業所銷退單 | 營業所銷退處理 |

---
