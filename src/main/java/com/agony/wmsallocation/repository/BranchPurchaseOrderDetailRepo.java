package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.purchase.BranchPurchaseOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchPurchaseOrderDetailRepo extends JpaRepository<BranchPurchaseOrderDetail, Integer> {

    List<BranchPurchaseOrderDetail> findByBpoNoOrderByItemNo(String bpoNo);
}
