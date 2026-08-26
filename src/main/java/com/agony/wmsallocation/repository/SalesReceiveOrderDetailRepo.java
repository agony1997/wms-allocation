package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.allocation.SalesReceiveOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesReceiveOrderDetailRepo extends JpaRepository<SalesReceiveOrderDetail, Integer> {

    List<SalesReceiveOrderDetail> findByReceiveNoOrderByItemNo(String receiveNo);
}
