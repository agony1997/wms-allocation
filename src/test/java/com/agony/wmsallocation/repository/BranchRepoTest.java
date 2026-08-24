package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.config.JpaAuditingConfig;
import com.agony.wmsallocation.entity.branch.Branch;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import com.agony.wmsallocation.security.UserContextHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = {
    "spring.sql.init.mode=never",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BranchRepoTest {

    // Testcontainers JUnit 5 Extension 會自動管理容器的 start/stop，不需要 try-with-resources。
    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
            .acceptLicense();

    @Autowired
    private BranchRepo branchRepo;

    @Test
    @DisplayName("findActiveBranchesByNameContaining - 名稱包含關鍵字且啟用中 - 只應回傳啟用的符合項")
    void findActiveBranchesByNameContaining_whenActiveAndNameMatches_shouldReturnOnlyActiveMatch() {
        // Arrange
        Branch b1 = new Branch();
        b1.setBranchCode("B001");
        b1.setSalesOrgCode("S01");
        b1.setBranchName("台北總部");
        b1.setStatus(ActiveStatus.ACTIVE);
        branchRepo.save(b1);

        Branch b2 = new Branch();
        b2.setBranchCode("B002");
        b2.setSalesOrgCode("S01");
        b2.setBranchName("台中分部");
        b2.setStatus(ActiveStatus.ACTIVE);
        branchRepo.save(b2);

        Branch b3 = new Branch();
        b3.setBranchCode("B003");
        b3.setSalesOrgCode("S01");
        b3.setBranchName("台北二部");
        b3.setStatus(ActiveStatus.INACTIVE);
        branchRepo.save(b3);

        // Act
        List<Branch> result = branchRepo.findActiveBranchesByNameContaining("台北");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBranchCode()).isEqualTo("B001");
    }

    @Test
    @DisplayName("save - 有登入者時 - createdBy 應為當前使用者，無登入者則回退 SYSTEM")
    void save_auditorAware_shouldUseLoggedInUserOrFallbackToSystem() {
        try {
            UserContextHolder.setUserCode("U001");
            Branch withUser = new Branch();
            withUser.setBranchCode("B101");
            withUser.setSalesOrgCode("S01");
            withUser.setBranchName("有登入者");
            withUser.setStatus(ActiveStatus.ACTIVE);
            branchRepo.save(withUser);
            assertThat(withUser.getCreatedBy()).isEqualTo("U001");
        } finally {
            UserContextHolder.clear();
        }

        Branch withoutUser = new Branch();
        withoutUser.setBranchCode("B102");
        withoutUser.setSalesOrgCode("S01");
        withoutUser.setBranchName("無登入者");
        withoutUser.setStatus(ActiveStatus.ACTIVE);
        branchRepo.save(withoutUser);
        assertThat(withoutUser.getCreatedBy()).isEqualTo("SYSTEM");
    }
}
