package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.purchase.BranchPurchaseFrozen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface BranchPurchaseFrozenRepo extends JpaRepository<BranchPurchaseFrozen, Integer> {

    // 判斷可否編輯只需這一筆：不存在＝可編輯，FROZEN/CONFIRMED＝不可編輯
    Optional<BranchPurchaseFrozen> findByBranchCodeAndPurchaseDate(String branchCode, LocalDate purchaseDate);
}
