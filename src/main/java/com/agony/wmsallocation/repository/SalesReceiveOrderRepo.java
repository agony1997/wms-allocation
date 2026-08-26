package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.allocation.SalesReceiveOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesReceiveOrderRepo extends JpaRepository<SalesReceiveOrder, Integer> {

    Optional<SalesReceiveOrder> findByReceiveNo(String receiveNo);

    List<SalesReceiveOrder> findByBranchCodeAndReceiveDate(String branchCode, LocalDate receiveDate);
}
