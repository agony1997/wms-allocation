package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.allocation.AllocationOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AllocationOrderRepo extends JpaRepository<AllocationOrder, Integer> {

    List<AllocationOrder> findByBranchCodeAndAllocationDate(String branchCode, LocalDate allocationDate);

    Optional<AllocationOrder> findByAllocationNo(String allocationNo);
}
