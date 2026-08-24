package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.inventory.InventoryTransaction;
import com.agony.wmsallocation.entity.inventory.enums.InventoryTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionRepo extends JpaRepository<InventoryTransaction, Integer> {

    /**
     * 查詢某儲位某產品的異動記錄
     */
    List<InventoryTransaction> findByBranchCodeAndLocationCodeAndProductCode(
            String branchCode, String locationCode, String productCode);

    /**
     * 依來源單據查詢異動記錄
     */
    List<InventoryTransaction> findBySourceDocTypeAndSourceDocNo(String sourceDocType, String sourceDocNo);

    /**
     * 依異動類型查詢
     */
    List<InventoryTransaction> findByTransactionType(InventoryTransactionType transactionType);

}
