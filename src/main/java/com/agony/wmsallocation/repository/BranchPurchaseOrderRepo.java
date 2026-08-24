package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.purchase.BranchPurchaseOrder;
import com.agony.wmsallocation.entity.purchase.enums.BpoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BranchPurchaseOrderRepo extends JpaRepository<BranchPurchaseOrder, Integer> {

    List<BranchPurchaseOrder> findByBranchCodeAndPurchaseDate(String branchCode, LocalDate purchaseDate);

    Optional<BranchPurchaseOrder> findByBpoNo(String bpoNo);

    List<BranchPurchaseOrder> findByBranchCodeAndStatusIn(String branchCode, Collection<BpoStatus> statuses);
}
