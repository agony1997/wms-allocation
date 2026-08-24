package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.branch.Branch;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepo extends JpaRepository<Branch, Integer> {

    Optional<Branch> findByBranchCode(String branchCode);

    boolean existsByBranchCode(String branchCode);

    boolean existsBySalesOrgCode(String salesOrgCode);

    List<Branch> findByStatus(ActiveStatus status);

    @Query("SELECT b FROM Branch b WHERE b.status = com.agony.wmsallocation.entity.enums.ActiveStatus.ACTIVE AND b.branchName LIKE %:name%")
    List<Branch> findActiveBranchesByNameContaining(@Param("name") String name);

}
