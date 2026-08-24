package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.branch.Location;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepo extends JpaRepository<Location, Integer> {

    boolean existsByBranchCode(String branchCode);

    List<Location> findByBranchCode(String branchCode);

    Optional<Location> findByBranchCodeAndLocationCode(String branchCode, String locationCode);

    List<Location> findByStatus(ActiveStatus status);

    List<Location> findByBranchCodeAndStatus(String branchCode, ActiveStatus status);

}
