package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.branch.SalesPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesPriorityRepo extends JpaRepository<SalesPriority, Integer> {

    List<SalesPriority> findByBranchCode(String branchCode);
}
