package com.agony.wmsallocation.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 覆蓋 9 個庫存異動方法的分支：找不到／數量不足／成功；
 * 寄庫三方法（keep/keepRetrieve/returnGoods）額外驗證雙儲位聯動的兩筆 transaction；
 * 每日快照驗「先刪後建」與欄位完整複製。
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    private static final String BRANCH = "B001";
    private static final String CAR_LOCATION = "S001";
    private static final String PRODUCT = "P001";
    private static final String BATCH = "BATCH01";
    private static final LocalDate EXPIRY = LocalDate.now().plusDays(30);

    /** 固定時鐘：快照的「今天」須來自注入的 Clock，不是 LocalDate.now()。 */
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-15T09:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 15);

    @Mock
    private InventoryRepo inventoryRepo;

    @Mock
    private InventoryTransactionRepo transactionRepo;

    @Mock
    private InventoryDailySnapshotRepo snapshotRepo;

    // 不用 @InjectMocks：Clock 是真實的 fixed 實例、不是 mock，得自己組（同 BranchPurchaseServiceTest）
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryRepo, transactionRepo, snapshotRepo, FIXED_CLOCK);
    }

    private Inventory stubInventory(String locationCode, LocationType type, int qty, int keepQty, int returnQty) {
        Inventory inv = new Inventory();
        inv.setBranchCode(BRANCH);
        inv.setLocationCode(locationCode);
        inv.setLocationType(type);
        inv.setProductCode(PRODUCT);
        inv.setBatchNo(BATCH);
        inv.setExpiryDate(EXPIRY);
        inv.setQty(qty);
        inv.setKeepQty(keepQty);
        inv.setReturnQty(returnQty);
        return inv;
    }

    private Inventory stubWarehouseInventory(int qty) {
        return stubInventory(BRANCH, LocationType.WAREHOUSE, qty, 0, 0);
    }

    private Inventory stubCarInventory(int qty) {
        return stubInventory(CAR_LOCATION, LocationType.CAR, qty, 0, 0);
    }

    private void mockFind(String locationCode, Optional<Inventory> result) {
        when(inventoryRepo.findByBranchCodeAndLocationCodeAndProductCodeAndBatchNo(BRANCH, locationCode, PRODUCT, BATCH))
                .thenReturn(result);
    }

    // 扣庫類走 getInventoryOrThrow -> 鎖定版 finder（ADR-0013）；入庫類走 findOrCreateInventory，仍用上面的 mockFind
    private void mockFindForUpdate(String locationCode, Optional<Inventory> result) {
        when(inventoryRepo.findForUpdateByBranchCodeAndLocationCodeAndProductCodeAndBatchNo(BRANCH, locationCode, PRODUCT, BATCH))
                .thenReturn(result);
    }

    // ==================== receive ====================

    @Test
    @DisplayName("receive 庫存不存在 - 應新建並以入庫量為初始 qty")
    void receive_whenNotExists_shouldCreateWithQty() {
        mockFind(BRANCH, Optional.empty());

        inventoryService.receive(BRANCH, PRODUCT, BATCH, EXPIRY, 10, "FDO001");

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepo).save(captor.capture());
        Inventory saved = captor.getValue();
        assertThat(saved.getQty()).isEqualTo(10);
        assertThat(saved.getLocationType()).isEqualTo(LocationType.WAREHOUSE);

        ArgumentCaptor<InventoryTransaction> txCaptor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepo).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getTransactionType()).isEqualTo(InventoryTransactionType.RECEIVE);
        assertThat(txCaptor.getValue().getQtyChange()).isEqualTo(10);
        assertThat(txCaptor.getValue().getSourceDocType()).isEqualTo("FDO");
    }

    @Test
    @DisplayName("receive 庫存已存在 - 應累加至現有 qty")
    void receive_whenExists_shouldAddToExistingQty() {
        Inventory existing = stubWarehouseInventory(20);
        mockFind(BRANCH, Optional.of(existing));

        inventoryService.receive(BRANCH, PRODUCT, BATCH, EXPIRY, 10, "FDO001");

        assertThat(existing.getQty()).isEqualTo(30);
        verify(inventoryRepo).save(existing);
    }

    // ==================== allocate ====================

    @Test
    @DisplayName("allocate 庫存記錄不存在 - 應拋出 ResourceNotFoundException")
    void allocate_whenInventoryNotFound_shouldThrowResourceNotFound() {
        mockFindForUpdate(BRANCH, Optional.empty());

        assertThatThrownBy(() -> inventoryService.allocate(BRANCH, PRODUCT, BATCH, EXPIRY, 10, "AO001"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(BRANCH)
                .hasMessageContaining(PRODUCT);
        verify(inventoryRepo, never()).save(any());
        verify(transactionRepo, never()).save(any());
    }

    @Test
    @DisplayName("allocate 庫存不足 - 應拋出 BusinessRuleException(INVENTORY_INSUFFICIENT) 且不異動")
    void allocate_whenInsufficientQty_shouldThrowInventoryInsufficient() {
        Inventory existing = stubWarehouseInventory(5);
        mockFindForUpdate(BRANCH, Optional.of(existing));

        assertThatThrownBy(() -> inventoryService.allocate(BRANCH, PRODUCT, BATCH, EXPIRY, 10, "AO001"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVENTORY_INSUFFICIENT);
        assertThat(existing.getQty()).isEqualTo(5);
        verify(inventoryRepo, never()).save(any());
        verify(transactionRepo, never()).save(any());
    }

    @Test
    @DisplayName("allocate 庫存充足 - 應扣減 qty 並寫入 ALLOCATE 異動")
    void allocate_whenSufficientQty_shouldDecreaseQty() {
        Inventory existing = stubWarehouseInventory(20);
        mockFindForUpdate(BRANCH, Optional.of(existing));

        inventoryService.allocate(BRANCH, PRODUCT, BATCH, EXPIRY, 10, "AO001");

        assertThat(existing.getQty()).isEqualTo(10);
        ArgumentCaptor<InventoryTransaction> txCaptor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepo).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getTransactionType()).isEqualTo(InventoryTransactionType.ALLOCATE);
        assertThat(txCaptor.getValue().getQtyChange()).isEqualTo(-10);
        assertThat(txCaptor.getValue().getSourceDocType()).isEqualTo("AO");
    }

    // ==================== pickUp ====================

    @Test
    @DisplayName("pickUp - 應於業務員儲位累加 qty")
    void pickUp_shouldAddQtyAtCarLocation() {
        Inventory existing = stubCarInventory(5);
        mockFind(CAR_LOCATION, Optional.of(existing));

        inventoryService.pickUp(BRANCH, CAR_LOCATION, PRODUCT, BATCH, EXPIRY, 10, "SRO001");

        assertThat(existing.getQty()).isEqualTo(15);
        ArgumentCaptor<InventoryTransaction> txCaptor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepo).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getTransactionType()).isEqualTo(InventoryTransactionType.PICK_UP);
        assertThat(txCaptor.getValue().getLocationType()).isEqualTo(LocationType.CAR);
        assertThat(txCaptor.getValue().getSourceDocType()).isEqualTo("SRO");
    }

    // ==================== sell ====================

    @Test
    @DisplayName("sell 業務員儲位無庫存記錄 - 應拋出 ResourceNotFoundException")
    void sell_whenNotFound_shouldThrowResourceNotFound() {
        mockFindForUpdate(CAR_LOCATION, Optional.empty());

        assertThatThrownBy(() -> inventoryService.sell(BRANCH, CAR_LOCATION, PRODUCT, BATCH, EXPIRY, 10, "SDO001"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("sell 車存不足 - 應拋出 BusinessRuleException(INVENTORY_INSUFFICIENT)")
    void sell_whenInsufficientQty_shouldThrowInventoryInsufficient() {
        Inventory existing = stubCarInventory(5);
        mockFindForUpdate(CAR_LOCATION, Optional.of(existing));

        assertThatThrownBy(() -> inventoryService.sell(BRANCH, CAR_LOCATION, PRODUCT, BATCH, EXPIRY, 10, "SDO001"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVENTORY_INSUFFICIENT);
        verify(inventoryRepo, never()).save(any());
    }

    @Test
    @DisplayName("sell 車存充足 - 應扣減 qty 並寫入 SALES 異動")
    void sell_whenSufficientQty_shouldDecreaseQty() {
        Inventory existing = stubCarInventory(20);
        mockFindForUpdate(CAR_LOCATION, Optional.of(existing));

        inventoryService.sell(BRANCH, CAR_LOCATION, PRODUCT, BATCH, EXPIRY, 10, "SDO001");

        assertThat(existing.getQty()).isEqualTo(10);
        ArgumentCaptor<InventoryTransaction> txCaptor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepo).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getTransactionType()).isEqualTo(InventoryTransactionType.SALES);
        assertThat(txCaptor.getValue().getQtyChange()).isEqualTo(-10);
        assertThat(txCaptor.getValue().getSourceDocType()).isEqualTo("SDO");
    }

    // ==================== customerReturn ====================

    @Test
    @DisplayName("customerReturn - 應於業務員儲位累加 qty")
    void customerReturn_shouldAddQtyAtCarLocation() {
        Inventory existing = stubCarInventory(5);
        mockFind(CAR_LOCATION, Optional.of(existing));

        inventoryService.customerReturn(BRANCH, CAR_LOCATION, PRODUCT, BATCH, EXPIRY, 10, "SDO002");

        assertThat(existing.getQty()).isEqualTo(15);
        ArgumentCaptor<InventoryTransaction> txCaptor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepo).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getTransactionType()).isEqualTo(InventoryTransactionType.CUSTOMER_RETURN);
        assertThat(txCaptor.getValue().getSourceDocType()).isEqualTo("SDO");
    }

    // ==================== keep（寄庫：業務員 qty-，大庫 keepQty+） ====================

    @Test
    @DisplayName("keep 業務員儲位無庫存記錄 - 應拋出 ResourceNotFoundException 且不異動")
    void keep_whenCarInventoryNotFound_shouldThrow() {
        mockFindForUpdate(CAR_LOCATION, Optional.empty());

        assertThatThrownBy(() -> inventoryService.keep(BRANCH, CAR_LOCATION, PRODUCT, BATCH, EXPIRY, 10, "SKR001"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(inventoryRepo, never()).save(any());
        verify(transactionRepo, never()).save(any());
    }

    @Test
    @DisplayName("keep 車存不足 - 應拋出 BusinessRuleException(INVENTORY_INSUFFICIENT) 且不異動大庫")
    void keep_whenCarQtyInsufficient_shouldThrow() {
        Inventory carInv = stubCarInventory(5);
        mockFindForUpdate(CAR_LOCATION, Optional.of(carInv));

        assertThatThrownBy(() -> inventoryService.keep(BRANCH, CAR_LOCATION, PRODUCT, BATCH, EXPIRY, 10, "SKR001"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVENTORY_INSUFFICIENT);
        verify(inventoryRepo, never()).save(any());
        verify(transactionRepo, never()).save(any());
    }

    @Test
    @DisplayName("keep 車存充足 - 應把車存 qty 轉為大庫 keepQty，各寫一筆異動")
    void keep_whenSufficient_shouldMoveCarQtyToWarehouseKeepQty() {
        Inventory carInv = stubCarInventory(20);
        Inventory whInv = stubInventory(BRANCH, LocationType.WAREHOUSE, 0, 3, 0);
        mockFindForUpdate(CAR_LOCATION, Optional.of(carInv));
        mockFind(BRANCH, Optional.of(whInv));

        inventoryService.keep(BRANCH, CAR_LOCATION, PRODUCT, BATCH, EXPIRY, 10, "SKR001");

        assertThat(carInv.getQty()).isEqualTo(10);
        assertThat(whInv.getKeepQty()).isEqualTo(13);

        ArgumentCaptor<InventoryTransaction> txCaptor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepo, times(2)).save(txCaptor.capture());
        List<InventoryTransaction> txs = txCaptor.getAllValues();

        assertThat(txs.get(0).getLocationType()).isEqualTo(LocationType.CAR);
        assertThat(txs.get(0).getTransactionType()).isEqualTo(InventoryTransactionType.KEEP);
        assertThat(txs.get(0).getQtyChange()).isEqualTo(-10);
        assertThat(txs.get(0).getKeepQtyChange()).isEqualTo(0);

        assertThat(txs.get(1).getLocationType()).isEqualTo(LocationType.WAREHOUSE);
        assertThat(txs.get(1).getTransactionType()).isEqualTo(InventoryTransactionType.KEEP);
        assertThat(txs.get(1).getQtyChange()).isEqualTo(0);
        assertThat(txs.get(1).getKeepQtyChange()).isEqualTo(10);
    }

    // ==================== keepRetrieve（領回寄庫：大庫 keepQty-，業務員 qty+） ====================

    @Test
    @DisplayName("keepRetrieve 大庫無庫存記錄 - 應拋出 ResourceNotFoundException")
    void keepRetrieve_whenWarehouseInventoryNotFound_shouldThrow() {
        mockFindForUpdate(BRANCH, Optional.empty());

        assertThatThrownBy(() -> inventoryService.keepRetrieve(BRANCH, CAR_LOCATION, PRODUCT, BATCH, EXPIRY, 10, "SRO002"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("keepRetrieve 寄庫量不足 - 應拋出 BusinessRuleException(INVENTORY_INSUFFICIENT)")
    void keepRetrieve_whenKeepQtyInsufficient_shouldThrow() {
        Inventory whInv = stubInventory(BRANCH, LocationType.WAREHOUSE, 0, 5, 0);
        mockFindForUpdate(BRANCH, Optional.of(whInv));

        assertThatThrownBy(() -> inventoryService.keepRetrieve(BRANCH, CAR_LOCATION, PRODUCT, BATCH, EXPIRY, 10, "SRO002"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVENTORY_INSUFFICIENT);
        verify(inventoryRepo, never()).save(any());
    }

    @Test
    @DisplayName("keepRetrieve 寄庫量充足 - 應把大庫 keepQty 轉為車存 qty，各寫一筆異動")
    void keepRetrieve_whenSufficient_shouldMoveWarehouseKeepQtyToCarQty() {
        Inventory whInv = stubInventory(BRANCH, LocationType.WAREHOUSE, 0, 20, 0);
        Inventory carInv = stubCarInventory(3);
        mockFindForUpdate(BRANCH, Optional.of(whInv));
        mockFind(CAR_LOCATION, Optional.of(carInv));

        inventoryService.keepRetrieve(BRANCH, CAR_LOCATION, PRODUCT, BATCH, EXPIRY, 10, "SRO002");

        assertThat(whInv.getKeepQty()).isEqualTo(10);
        assertThat(carInv.getQty()).isEqualTo(13);

        ArgumentCaptor<InventoryTransaction> txCaptor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepo, times(2)).save(txCaptor.capture());
        List<InventoryTransaction> txs = txCaptor.getAllValues();

        assertThat(txs.get(0).getLocationType()).isEqualTo(LocationType.WAREHOUSE);
        assertThat(txs.get(0).getKeepQtyChange()).isEqualTo(-10);
        assertThat(txs.get(0).getQtyChange()).isEqualTo(0);

        assertThat(txs.get(1).getLocationType()).isEqualTo(LocationType.CAR);
        assertThat(txs.get(1).getQtyChange()).isEqualTo(10);
    }

    // ==================== returnGoods（退庫：業務員 qty-，大庫 returnQty+） ====================

    @Test
    @DisplayName("returnGoods 業務員儲位無庫存記錄 - 應拋出 ResourceNotFoundException")
    void returnGoods_whenCarInventoryNotFound_shouldThrow() {
        mockFindForUpdate(CAR_LOCATION, Optional.empty());

        assertThatThrownBy(() -> inventoryService.returnGoods(BRANCH, CAR_LOCATION, PRODUCT, BATCH, EXPIRY, 10, "SRR001"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("returnGoods 車存不足 - 應拋出 BusinessRuleException(INVENTORY_INSUFFICIENT)")
    void returnGoods_whenCarQtyInsufficient_shouldThrow() {
        Inventory carInv = stubCarInventory(5);
        mockFindForUpdate(CAR_LOCATION, Optional.of(carInv));

        assertThatThrownBy(() -> inventoryService.returnGoods(BRANCH, CAR_LOCATION, PRODUCT, BATCH, EXPIRY, 10, "SRR001"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVENTORY_INSUFFICIENT);
        verify(inventoryRepo, never()).save(any());
    }

    @Test
    @DisplayName("returnGoods 車存充足 - 應把車存 qty 轉為大庫 returnQty，各寫一筆異動")
    void returnGoods_whenSufficient_shouldMoveCarQtyToWarehouseReturnQty() {
        Inventory carInv = stubCarInventory(20);
        Inventory whInv = stubInventory(BRANCH, LocationType.WAREHOUSE, 0, 0, 3);
        mockFindForUpdate(CAR_LOCATION, Optional.of(carInv));
        mockFind(BRANCH, Optional.of(whInv));

        inventoryService.returnGoods(BRANCH, CAR_LOCATION, PRODUCT, BATCH, EXPIRY, 10, "SRR001");

        assertThat(carInv.getQty()).isEqualTo(10);
        assertThat(whInv.getReturnQty()).isEqualTo(13);

        ArgumentCaptor<InventoryTransaction> txCaptor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepo, times(2)).save(txCaptor.capture());
        List<InventoryTransaction> txs = txCaptor.getAllValues();

        assertThat(txs.get(0).getLocationType()).isEqualTo(LocationType.CAR);
        assertThat(txs.get(0).getTransactionType()).isEqualTo(InventoryTransactionType.RETURN);
        assertThat(txs.get(0).getQtyChange()).isEqualTo(-10);

        assertThat(txs.get(1).getLocationType()).isEqualTo(LocationType.WAREHOUSE);
        assertThat(txs.get(1).getReturnQtyChange()).isEqualTo(10);
    }

    // ==================== returnShip（銷退送出：大庫 returnQty-） ====================

    @Test
    @DisplayName("returnShip 大庫無庫存記錄 - 應拋出 ResourceNotFoundException")
    void returnShip_whenNotFound_shouldThrow() {
        mockFindForUpdate(BRANCH, Optional.empty());

        assertThatThrownBy(() -> inventoryService.returnShip(BRANCH, PRODUCT, BATCH, EXPIRY, 10, "BRO001"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("returnShip 待退庫量不足 - 應拋出 BusinessRuleException(INVENTORY_INSUFFICIENT)")
    void returnShip_whenReturnQtyInsufficient_shouldThrow() {
        Inventory inv = stubInventory(BRANCH, LocationType.WAREHOUSE, 0, 0, 5);
        mockFindForUpdate(BRANCH, Optional.of(inv));

        assertThatThrownBy(() -> inventoryService.returnShip(BRANCH, PRODUCT, BATCH, EXPIRY, 10, "BRO001"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVENTORY_INSUFFICIENT);
        verify(inventoryRepo, never()).save(any());
    }

    @Test
    @DisplayName("returnShip 待退庫量充足 - 應扣減 returnQty 並寫入 RETURN_SHIP 異動")
    void returnShip_whenSufficient_shouldDecreaseReturnQty() {
        Inventory inv = stubInventory(BRANCH, LocationType.WAREHOUSE, 0, 0, 20);
        mockFindForUpdate(BRANCH, Optional.of(inv));

        inventoryService.returnShip(BRANCH, PRODUCT, BATCH, EXPIRY, 10, "BRO001");

        assertThat(inv.getReturnQty()).isEqualTo(10);
        ArgumentCaptor<InventoryTransaction> txCaptor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(transactionRepo).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getTransactionType()).isEqualTo(InventoryTransactionType.RETURN_SHIP);
        assertThat(txCaptor.getValue().getReturnQtyChange()).isEqualTo(-10);
        assertThat(txCaptor.getValue().getSourceDocType()).isEqualTo("BRO");
    }

    // ==================== 每日快照 ====================

    @Test
    @DisplayName("createTodaySnapshot - 今天應取自注入的 Clock，並回傳該日期供組 Location")
    void createTodaySnapshot_shouldUseInjectedClockAndReturnDate() {
        LocalDate result = inventoryService.createTodaySnapshot();

        assertThat(result).isEqualTo(TODAY);
        verify(snapshotRepo).deleteBySnapshotDate(TODAY);
    }

    @Test
    @DisplayName("createDailySnapshot - 應先刪當日舊快照再寫新的（重跑冪等，不得殘留舊列）")
    void createDailySnapshot_shouldDeleteBeforeSave() {
        when(inventoryRepo.findAll()).thenReturn(List.of(stubWarehouseInventory(20)));

        inventoryService.createDailySnapshot(TODAY);

        InOrder inOrder = inOrder(snapshotRepo);
        inOrder.verify(snapshotRepo).deleteBySnapshotDate(TODAY);
        inOrder.verify(snapshotRepo).saveAll(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("createDailySnapshot - 三種數量與批次欄位應完整複製到快照")
    void createDailySnapshot_shouldCopyAllFields() {
        Inventory inv = stubInventory(BRANCH, LocationType.WAREHOUSE, 20, 5, 3);
        when(inventoryRepo.findAll()).thenReturn(List.of(inv));

        inventoryService.createDailySnapshot(TODAY);

        ArgumentCaptor<List<InventoryDailySnapshot>> captor = ArgumentCaptor.forClass(List.class);
        verify(snapshotRepo).saveAll(captor.capture());

        assertThat(captor.getValue()).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.getSnapshotDate()).isEqualTo(TODAY);
            assertThat(snapshot.getBranchCode()).isEqualTo(BRANCH);
            assertThat(snapshot.getLocationCode()).isEqualTo(BRANCH);
            assertThat(snapshot.getLocationType()).isEqualTo(LocationType.WAREHOUSE);
            assertThat(snapshot.getProductCode()).isEqualTo(PRODUCT);
            assertThat(snapshot.getBatchNo()).isEqualTo(BATCH);
            assertThat(snapshot.getExpiryDate()).isEqualTo(EXPIRY);
            assertThat(snapshot.getQty()).isEqualTo(20);
            assertThat(snapshot.getKeepQty()).isEqualTo(5);
            assertThat(snapshot.getReturnQty()).isEqualTo(3);
        });
    }
}
