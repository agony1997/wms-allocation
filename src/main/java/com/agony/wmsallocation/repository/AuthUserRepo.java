package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.auth.AuthUser;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthUserRepo extends JpaRepository<AuthUser, Integer> {

    Optional<AuthUser> findByUserCode(String userCode);

    boolean existsByUserCode(String userCode);

    boolean existsByBranchCode(String branchCode);

    List<AuthUser> findByStatus(ActiveStatus status);

}
