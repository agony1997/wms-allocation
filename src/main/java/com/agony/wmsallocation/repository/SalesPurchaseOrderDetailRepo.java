package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.purchase.SalesPurchaseOrderDetail;
import com.agony.wmsallocation.entity.purchase.enums.SalesOrderDetailStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SalesPurchaseOrderDetailRepo extends JpaRepository<SalesPurchaseOrderDetail, Integer> {

    List<SalesPurchaseOrderDetail> findByPurchaseNoOrderBySortOrder(String purchaseNo);

    void deleteByPurchaseNo(String purchaseNo);

    List<SalesPurchaseOrderDetail> findByPurchaseNoInAndStatus(List<String> purchaseNos, SalesOrderDetailStatus status);

    // 悲觀鎖鎖住待配 SPOD 這批列（比照 ADR-0013），避免同營業所配貨被連點/逾時重試時重複配貨；鎖撐多久由呼叫端 Service 的 @Transactional 決定
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<SalesPurchaseOrderDetail> findByBpoNoInAndStatus(Collection<String> bpoNos, SalesOrderDetailStatus status);
}
