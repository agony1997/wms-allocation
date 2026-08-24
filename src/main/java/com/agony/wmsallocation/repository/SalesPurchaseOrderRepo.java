package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.purchase.SalesPurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesPurchaseOrderRepo extends JpaRepository<SalesPurchaseOrder, Integer> {

    // 查詢/自動建單：每儲位每訂貨日僅一筆
    Optional<SalesPurchaseOrder> findByBranchCodeAndLocationCodeAndPurchaseDate(String branchCode, String locationCode, LocalDate purchaseDate);

    Optional<SalesPurchaseOrder> findByPurchaseNo(String purchaseNo);

    List<SalesPurchaseOrder> findByBranchCodeAndPurchaseDate(String branchCode, LocalDate purchaseDate);

    List<SalesPurchaseOrder> findByPurchaseNoIn(Collection<String> purchaseNos);
}
