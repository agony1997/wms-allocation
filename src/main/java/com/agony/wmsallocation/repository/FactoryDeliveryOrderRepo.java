package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.receive.FactoryDeliveryOrder;
import com.agony.wmsallocation.entity.receive.enums.FactoryDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FactoryDeliveryOrderRepo extends JpaRepository<FactoryDeliveryOrder, Integer> {

    boolean existsByBpoNo(String bpoNo);

    Optional<FactoryDeliveryOrder> findByFdoNo(String fdoNo);

    List<FactoryDeliveryOrder> findByBranchCodeAndStatusIn(String branchCode, List<FactoryDeliveryStatus> statuses);
}
