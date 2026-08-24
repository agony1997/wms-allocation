package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.auth.AuthUserBranchRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthUserBranchRoleRepo extends JpaRepository<AuthUserBranchRole, Integer> {

    List<AuthUserBranchRole> findByUserCode(String userCode);

}
