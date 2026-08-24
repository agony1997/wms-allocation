package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.branch.enums.LocationType;
import com.agony.wmsallocation.entity.inventory.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepo extends JpaRepository<Inventory, Integer> {

    /**
     * 查詢大庫庫存（locationType = WAREHOUSE）
     */
    List<Inventory> findByBranchCodeAndLocationType(String branchCode, LocationType locationType);

    /**
     * 查詢某儲位的庫存
     */
    List<Inventory> findByLocationCode(String locationCode);

    /**
     * 查詢某產品在所有儲位的庫存分布
     */
    List<Inventory> findByProductCode(String productCode);

    /**
     * 精確查詢特定庫存記錄（餘額表更新用）
     */
    Optional<Inventory> findByBranchCodeAndLocationCodeAndProductCodeAndBatchNo(
            String branchCode,
            String locationCode,
            String productCode,
            String batchNo);

    /**
     * 精確查詢並對該列上悲觀寫鎖（扣庫類操作用，ADR-0013）；鎖撐多久由呼叫端 Service 的 @Transactional 決定。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findForUpdateByBranchCodeAndLocationCodeAndProductCodeAndBatchNo(
            String branchCode,
            String locationCode,
            String productCode,
            String batchNo);

}
