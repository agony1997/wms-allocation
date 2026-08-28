package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.auth.AuthUserBranchRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthUserBranchRoleRepo extends JpaRepository<AuthUserBranchRole, Integer> {

    List<AuthUserBranchRole> findByUserCode(String userCode);

    /**
     * 該營業所底下是否還有人員（角色關聯）。營業所刪除前的引用檢查用。
     *
     * <p>不可改查 {@code AuthUser}——使用者本身已不掛營業所欄位，
     * 「誰隸屬這個所」的唯一真相就在本表。
     */
    boolean existsByBranchCode(String branchCode);

}
