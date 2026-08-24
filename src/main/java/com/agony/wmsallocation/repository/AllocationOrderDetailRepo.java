package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.allocation.AllocationOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllocationOrderDetailRepo extends JpaRepository<AllocationOrderDetail, Integer> {

    List<AllocationOrderDetail> findByAllocationNoOrderByItemNo(String allocationNo);
}
