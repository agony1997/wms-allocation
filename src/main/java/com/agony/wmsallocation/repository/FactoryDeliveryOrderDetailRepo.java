package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.receive.FactoryDeliveryOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FactoryDeliveryOrderDetailRepo extends JpaRepository<FactoryDeliveryOrderDetail, Integer> {

    List<FactoryDeliveryOrderDetail> findByFdoNoOrderByItemNo(String fdoNo);
}
