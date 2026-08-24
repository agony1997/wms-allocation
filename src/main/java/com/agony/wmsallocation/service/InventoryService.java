package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.inventory.InventoryDto;
import com.agony.wmsallocation.dto.inventory.InventoryTransactionDto;
import com.agony.wmsallocation.entity.branch.enums.LocationType;
import com.agony.wmsallocation.entity.inventory.Inventory;
import com.agony.wmsallocation.entity.inventory.InventoryDailySnapshot;
import com.agony.wmsallocation.entity.inventory.InventoryTransaction;
import com.agony.wmsallocation.entity.inventory.enums.InventoryTransactionType;
import com.agony.wmsallocation.exception.BusinessRuleException;
import com.agony.wmsallocation.exception.ErrorCode;
import com.agony.wmsallocation.exception.ResourceNotFoundException;
import com.agony.wmsallocation.repository.InventoryDailySnapshotRepo;
import com.agony.wmsallocation.repository.InventoryRepo;
import com.agony.wmsallocation.repository.InventoryTransactionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
public class InventoryService {

    private final InventoryRepo inventoryRepo;
    private final InventoryTransactionRepo transactionRepo;
    private final InventoryDailySnapshotRepo snapshotRepo;
    private final Clock clock;

    // ==================== 查詢方法 ====================

    /**
     * 查詢所有庫存
     */
    public List<InventoryDto> findAll() {
        return inventoryRepo.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 查詢大庫庫存
     */
    public List<InventoryDto> findWarehouseInventory(String branchCode) {
        return inventoryRepo.findByBranchCodeAndLocationType(branchCode, LocationType.WAREHOUSE).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 查詢某儲位庫存
     */
    public List<InventoryDto> findByLocation(String locationCode) {
        return inventoryRepo.findByLocationCode(locationCode).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 查詢某產品庫存分布
     */
    public List<InventoryDto> findByProduct(String productCode) {
        return inventoryRepo.findByProductCode(productCode).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 查詢某單據的異動記錄
     */
    public List<InventoryTransactionDto> findTransactionsByDoc(String sourceDocType, String sourceDocNo) {
        return transactionRepo.findBySourceDocTypeAndSourceDocNo(sourceDocType, sourceDocNo).stream()
                .map(this::toTransactionDto)
                .toList();
    }

    // ==================== 庫存異動方法 ====================

    /**
     * 收貨入庫：大庫 qty +
     */
    @Transactional
    public void receive(String branchCode, String productCode, String batchNo,
                        LocalDate expiryDate, int qty, String sourceDocNo) {
        Inventory inv = findOrCreateInventory(branchCode, branchCode, LocationType.WAREHOUSE,
                productCode, batchNo, expiryDate);
        inv.setQty(inv.getQty() + qty);
        inventoryRepo.save(inv);

        saveTransaction(branchCode, branchCode, LocationType.WAREHOUSE, productCode, batchNo,
                expiryDate, InventoryTransactionType.RECEIVE, qty, 0, 0, "FDO", sourceDocNo);
    }

    /**
     * 配貨扣庫：大庫 qty -
     */
    @Transactional
    public void allocate(String branchCode, String productCode, String batchNo,
                         LocalDate expiryDate, int qty, String sourceDocNo) {
        Inventory inv = getInventoryOrThrow(branchCode, branchCode, productCode, batchNo);
        validateSufficientQty(inv.getQty(), qty, "配貨");
        inv.setQty(inv.getQty() - qty);
        inventoryRepo.save(inv);

        saveTransaction(branchCode, branchCode, LocationType.WAREHOUSE, productCode, batchNo,
                expiryDate, InventoryTransactionType.ALLOCATE, -qty, 0, 0, "AO", sourceDocNo);
    }

    /**
     * 業務員領貨：業務員儲位 qty +
     */
    @Transactional
    public void pickUp(String branchCode, String locationCode, String productCode,
                       String batchNo, LocalDate expiryDate, int qty, String sourceDocNo) {
        Inventory inv = findOrCreateInventory(branchCode, locationCode, LocationType.CAR,
                productCode, batchNo, expiryDate);
        inv.setQty(inv.getQty() + qty);
        inventoryRepo.save(inv);

        saveTransaction(branchCode, locationCode, LocationType.CAR, productCode, batchNo,
                expiryDate, InventoryTransactionType.PICK_UP, qty, 0, 0, "SRO", sourceDocNo);
    }

    /**
     * 銷售出庫：業務員儲位 qty -
     */
    @Transactional
    public void sell(String branchCode, String locationCode, String productCode,
                     String batchNo, LocalDate expiryDate, int qty, String sourceDocNo) {
        Inventory inv = getInventoryOrThrow(branchCode, locationCode, productCode, batchNo);
        validateSufficientQty(inv.getQty(), qty, "銷售");
        inv.setQty(inv.getQty() - qty);
        inventoryRepo.save(inv);

        saveTransaction(branchCode, locationCode, LocationType.CAR, productCode, batchNo,
                expiryDate, InventoryTransactionType.SALES, -qty, 0, 0, "SDO", sourceDocNo);
    }

    /**
     * 客戶退貨：業務員儲位 qty +
     */
    @Transactional
    public void customerReturn(String branchCode, String locationCode, String productCode,
                               String batchNo, LocalDate expiryDate, int qty, String sourceDocNo) {
        Inventory inv = findOrCreateInventory(branchCode, locationCode, LocationType.CAR,
                productCode, batchNo, expiryDate);
        inv.setQty(inv.getQty() + qty);
        inventoryRepo.save(inv);

        saveTransaction(branchCode, locationCode, LocationType.CAR, productCode, batchNo,
                expiryDate, InventoryTransactionType.CUSTOMER_RETURN, qty, 0, 0, "SDO", sourceDocNo);
    }

    /**
     * 寄庫：業務員 qty - , 大庫 keepQty +
     */
    @Transactional
    public void keep(String branchCode, String locationCode, String productCode,
                     String batchNo, LocalDate expiryDate, int qty, String sourceDocNo) {
        // 業務員儲位 qty -
        Inventory carInv = getInventoryOrThrow(branchCode, locationCode, productCode, batchNo);
        validateSufficientQty(carInv.getQty(), qty, "寄庫");
        carInv.setQty(carInv.getQty() - qty);
        inventoryRepo.save(carInv);

        saveTransaction(branchCode, locationCode, LocationType.CAR, productCode, batchNo,
                expiryDate, InventoryTransactionType.KEEP, -qty, 0, 0, "SKR", sourceDocNo);

        // 大庫 keepQty +
        Inventory whInv = findOrCreateInventory(branchCode, branchCode, LocationType.WAREHOUSE,
                productCode, batchNo, expiryDate);
        whInv.setKeepQty(whInv.getKeepQty() + qty);
        inventoryRepo.save(whInv);

        saveTransaction(branchCode, branchCode, LocationType.WAREHOUSE, productCode, batchNo,
                expiryDate, InventoryTransactionType.KEEP, 0, qty, 0, "SKR", sourceDocNo);
    }

    /**
     * 領回寄庫：大庫 keepQty - , 業務員 qty +
     */
    @Transactional
    public void keepRetrieve(String branchCode, String locationCode, String productCode,
                             String batchNo, LocalDate expiryDate, int qty, String sourceDocNo) {
        // 大庫 keepQty -
        Inventory whInv = getInventoryOrThrow(branchCode, branchCode, productCode, batchNo);
        validateSufficientQty(whInv.getKeepQty(), qty, "領回寄庫");
        whInv.setKeepQty(whInv.getKeepQty() - qty);
        inventoryRepo.save(whInv);

        saveTransaction(branchCode, branchCode, LocationType.WAREHOUSE, productCode, batchNo,
                expiryDate, InventoryTransactionType.KEEP_RETRIEVE, 0, -qty, 0, "SRO", sourceDocNo);

        // 業務員 qty +
        Inventory carInv = findOrCreateInventory(branchCode, locationCode, LocationType.CAR,
                productCode, batchNo, expiryDate);
        carInv.setQty(carInv.getQty() + qty);
        inventoryRepo.save(carInv);

        saveTransaction(branchCode, locationCode, LocationType.CAR, productCode, batchNo,
                expiryDate, InventoryTransactionType.KEEP_RETRIEVE, qty, 0, 0, "SRO", sourceDocNo);
    }

    /**
     * 退庫：業務員 qty - , 大庫 returnQty +
     */
    @Transactional
    public void returnGoods(String branchCode, String locationCode, String productCode,
                            String batchNo, LocalDate expiryDate, int qty, String sourceDocNo) {
        // 業務員儲位 qty -
        Inventory carInv = getInventoryOrThrow(branchCode, locationCode, productCode, batchNo);
        validateSufficientQty(carInv.getQty(), qty, "退庫");
        carInv.setQty(carInv.getQty() - qty);
        inventoryRepo.save(carInv);

        saveTransaction(branchCode, locationCode, LocationType.CAR, productCode, batchNo,
                expiryDate, InventoryTransactionType.RETURN, -qty, 0, 0, "SRR", sourceDocNo);

        // 大庫 returnQty +
        Inventory whInv = findOrCreateInventory(branchCode, branchCode, LocationType.WAREHOUSE,
                productCode, batchNo, expiryDate);
        whInv.setReturnQty(whInv.getReturnQty() + qty);
        inventoryRepo.save(whInv);

        saveTransaction(branchCode, branchCode, LocationType.WAREHOUSE, productCode, batchNo,
                expiryDate, InventoryTransactionType.RETURN, 0, 0, qty, "SRR", sourceDocNo);
    }

    /**
     * 銷退送出：大庫 returnQty -
     */
    @Transactional
    public void returnShip(String branchCode, String productCode, String batchNo,
                           LocalDate expiryDate, int qty, String sourceDocNo) {
        Inventory inv = getInventoryOrThrow(branchCode, branchCode, productCode, batchNo);
        validateSufficientQty(inv.getReturnQty(), qty, "銷退送出");
        inv.setReturnQty(inv.getReturnQty() - qty);
        inventoryRepo.save(inv);

        saveTransaction(branchCode, branchCode, LocationType.WAREHOUSE, productCode, batchNo,
                expiryDate, InventoryTransactionType.RETURN_SHIP, 0, 0, -qty, "BRO", sourceDocNo);
    }

    // ==================== 每日快照 ====================

    /**
     * 產生系統當日快照（@Scheduled 自動 + 手動 API 共用入口）。
     *
     * <p>「今天」一律取自注入的 {@link Clock}，不用 {@code LocalDate.now()}——測試才能以
     * {@code Clock.fixed(...)} 固定時間、可重現（同 {@code SalesPurchaseService} 等）。
     *
     * @return 本次快照的日期，供呼叫端組 Location／回報
     */
    @Transactional
    public LocalDate createTodaySnapshot() {
        LocalDate today = LocalDate.now(clock);
        createDailySnapshot(today);
        return today;
    }

    /**
     * 產生指定日快照。當天已有快照則先刪除再重建（重跑冪等）。
     */
    @Transactional
    public void createDailySnapshot(LocalDate date) {
        // 若當天已有快照，先刪除再重建
        snapshotRepo.deleteBySnapshotDate(date);

        List<InventoryDailySnapshot> snapshots = inventoryRepo.findAll().stream()
                .map(inv -> {
                    InventoryDailySnapshot snapshot = new InventoryDailySnapshot();
                    snapshot.setSnapshotDate(date);
                    snapshot.setBranchCode(inv.getBranchCode());
                    snapshot.setLocationCode(inv.getLocationCode());
                    snapshot.setLocationType(inv.getLocationType());
                    snapshot.setProductCode(inv.getProductCode());
                    snapshot.setBatchNo(inv.getBatchNo());
                    snapshot.setExpiryDate(inv.getExpiryDate());
                    snapshot.setQty(inv.getQty());
                    snapshot.setKeepQty(inv.getKeepQty());
                    snapshot.setReturnQty(inv.getReturnQty());
                    return snapshot;
                })
                .toList();

        snapshotRepo.saveAll(snapshots);
    }

    /**
     * 每日 23:59 自動產生快照。
     *
     * <p>{@code @Transactional} 必須標在**這裡**：底下對 {@code createTodaySnapshot()} 是自我呼叫
     * （this.xxx()，不經過 Spring proxy），該方法自己的 {@code @Transactional} 不會生效。
     * 少了這個標註，刪舊快照與寫新快照會落在兩個交易，中途失敗就留下當天沒有快照的空窗。
     */
    @Transactional
    @Scheduled(cron = "0 59 23 * * *")
    public void scheduledDailySnapshot() {
        createTodaySnapshot();
    }

    /**
     * 查詢歷史快照
     */
    public List<InventoryDto> findSnapshotByDate(LocalDate date) {
        return snapshotRepo.findBySnapshotDate(date).stream()
                .map(this::snapshotToDto)
                .toList();
    }

    /**
     * 查詢某天某營業所的快照
     */
    public List<InventoryDto> findSnapshotByDateAndBranch(LocalDate date, String branchCode) {
        return snapshotRepo.findBySnapshotDateAndBranchCode(date, branchCode).stream()
                .map(this::snapshotToDto)
                .toList();
    }

    // ==================== 私有方法 ====================

    /**
     * 查詢庫存記錄，不存在則新建（用於入庫類操作）
     */
    private Inventory findOrCreateInventory(String branchCode, String locationCode,
                                            LocationType locationType, String productCode,
                                            String batchNo, LocalDate expiryDate) {
        return inventoryRepo.findByBranchCodeAndLocationCodeAndProductCodeAndBatchNo(
                branchCode, locationCode, productCode, batchNo
        ).orElseGet(() -> {
            Inventory inv = new Inventory();
            inv.setBranchCode(branchCode);
            inv.setLocationCode(locationCode);
            inv.setLocationType(locationType);
            inv.setProductCode(productCode);
            inv.setBatchNo(batchNo);
            inv.setExpiryDate(expiryDate);
            inv.setQty(0);
            inv.setKeepQty(0);
            inv.setReturnQty(0);
            return inv;
        });
    }

    /**
     * 查詢庫存記錄，不存在則拋出例外（用於扣庫類操作）
     */
    private Inventory getInventoryOrThrow(String branchCode, String locationCode,
                                          String productCode, String batchNo) {
        return inventoryRepo.findForUpdateByBranchCodeAndLocationCodeAndProductCodeAndBatchNo(branchCode, locationCode,
                        productCode, batchNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("庫存記錄不存在：branchCode=%s, locationCode=%s, productCode=%s, batchNo=%s",
                                branchCode, locationCode, productCode, batchNo)));
    }

    /**
     * 驗證數量是否足夠
     */
    private void validateSufficientQty(int currentQty, int requiredQty, String operation) {
        if (currentQty < requiredQty) {
            throw new BusinessRuleException(
                    String.format("%s失敗：庫存不足（現有 %d，需要 %d）", operation, currentQty, requiredQty),
                    ErrorCode.INVENTORY_INSUFFICIENT);
        }
    }

    /**
     * 儲存異動記錄
     */
    private void saveTransaction(String branchCode, String locationCode, LocationType locationType,
                                 String productCode, String batchNo, LocalDate expiryDate,
                                 InventoryTransactionType type, int qtyChange,
                                 int keepQtyChange, int returnQtyChange,
                                 String sourceDocType, String sourceDocNo) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setBranchCode(branchCode);
        tx.setLocationCode(locationCode);
        tx.setLocationType(locationType);
        tx.setProductCode(productCode);
        tx.setBatchNo(batchNo);
        tx.setExpiryDate(expiryDate);
        tx.setTransactionType(type);
        tx.setQtyChange(qtyChange);
        tx.setKeepQtyChange(keepQtyChange);
        tx.setReturnQtyChange(returnQtyChange);
        tx.setSourceDocType(sourceDocType);
        tx.setSourceDocNo(sourceDocNo);
        transactionRepo.save(tx);
    }

    private InventoryDto toDto(Inventory inv) {
        return InventoryDto.builder()
                .id(inv.getId())
                .branchCode(inv.getBranchCode())
                .locationCode(inv.getLocationCode())
                .locationType(inv.getLocationType())
                .productCode(inv.getProductCode())
                .batchNo(inv.getBatchNo())
                .expiryDate(inv.getExpiryDate())
                .qty(inv.getQty())
                .keepQty(inv.getKeepQty())
                .returnQty(inv.getReturnQty())
                .build();
    }

    private InventoryTransactionDto toTransactionDto(InventoryTransaction tx) {
        return InventoryTransactionDto.builder()
                .id(tx.getId())
                .branchCode(tx.getBranchCode())
                .locationCode(tx.getLocationCode())
                .locationType(tx.getLocationType())
                .productCode(tx.getProductCode())
                .batchNo(tx.getBatchNo())
                .expiryDate(tx.getExpiryDate())
                .transactionType(tx.getTransactionType())
                .qtyChange(tx.getQtyChange())
                .keepQtyChange(tx.getKeepQtyChange())
                .returnQtyChange(tx.getReturnQtyChange())
                .sourceDocType(tx.getSourceDocType())
                .sourceDocNo(tx.getSourceDocNo())
                .createdAt(tx.getCreatedAt())
                .createdBy(tx.getCreatedBy())
                .build();
    }

    private InventoryDto snapshotToDto(InventoryDailySnapshot snapshot) {
        return InventoryDto.builder()
                .id(snapshot.getId())
                .branchCode(snapshot.getBranchCode())
                .locationCode(snapshot.getLocationCode())
                .locationType(snapshot.getLocationType())
                .productCode(snapshot.getProductCode())
                .batchNo(snapshot.getBatchNo())
                .expiryDate(snapshot.getExpiryDate())
                .qty(snapshot.getQty())
                .keepQty(snapshot.getKeepQty())
                .returnQty(snapshot.getReturnQty())
                .build();
    }
}
