package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.enums.ActiveStatus;
import com.agony.wmsallocation.entity.master.Factory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FactoryRepo extends JpaRepository<Factory, Integer> {

    Optional<Factory> findByFactoryCode(String factoryCode);

    boolean existsByFactoryCode(String factoryCode);

    List<Factory> findByStatus(ActiveStatus status);

}
