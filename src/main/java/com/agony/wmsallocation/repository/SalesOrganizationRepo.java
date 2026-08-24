package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.enums.ActiveStatus;
import com.agony.wmsallocation.entity.master.SalesOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalesOrganizationRepo extends JpaRepository<SalesOrganization, Integer> {

    Optional<SalesOrganization> findBySalesOrgCode(String salesOrgCode);

    boolean existsBySalesOrgCode(String salesOrgCode);

    List<SalesOrganization> findByStatus(ActiveStatus status);

}
