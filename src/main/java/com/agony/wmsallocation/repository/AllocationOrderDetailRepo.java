package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.allocation.AllocationOrderDetail;
import com.agony.wmsallocation.entity.allocation.enums.AllocationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllocationOrderDetailRepo extends JpaRepository<AllocationOrderDetail, Integer> {

    List<AllocationOrderDetail> findByAllocationNoOrderByItemNo(String allocationNo);

    /**
     * 某業務員儲位的待領明細（唯讀預覽用，不上鎖）。
     * locationCode 全域唯一，單鍵即無歧義，不需再由 branchCode 收斂。
     */
    List<AllocationOrderDetail> findByLocationCodeAndStatusOrderByAllocationNoAscItemNoAsc(
            String locationCode, AllocationStatus status);

    /**
     * 同上，但對待領明細上悲觀寫鎖（確認領貨路徑用，比照 {@code SalesPurchaseOrderDetailRepo}
     * 的待配 SPOD）；避免連點／逾時重試把同一批貨領兩次進車存。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AllocationOrderDetail> findForUpdateByLocationCodeAndStatusOrderByAllocationNoAscItemNoAsc(
            String locationCode, AllocationStatus status);

    /** 判斷某配貨單是否仍有明細處於指定狀態，供維護 AO 聚合狀態（AllocationOrder.md 狀態約束）。 */
    boolean existsByAllocationNoAndStatus(String allocationNo, AllocationStatus status);
}
