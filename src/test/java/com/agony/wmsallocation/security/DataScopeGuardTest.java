package com.agony.wmsallocation.security;

import com.agony.wmsallocation.entity.branch.Location;
import com.agony.wmsallocation.exception.BusinessException;
import com.agony.wmsallocation.exception.ErrorCode;
import com.agony.wmsallocation.exception.ResourceNotFoundException;
import com.agony.wmsallocation.repository.LocationRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * User.md「資料範圍授權」段落定案的兩個共用入口，test-first（ADR-0005：真實業務規則）。
 */
@ExtendWith(MockitoExtension.class)
class DataScopeGuardTest {

    private static final String BRANCH = "B01";
    private static final String LOCATION = "L01";
    private static final String OPERATOR = "U01";

    @Mock LocationRepo locationRepo;

    private DataScopeGuard guard;

    @BeforeEach
    void setUp() {
        guard = new DataScopeGuard(locationRepo);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void assertBranchAccess_whenHasRequiredRole_doesNotThrow() {
        UserContextHolder.setBranchRoles(Map.of(BRANCH, Set.of("LEADER")));

        Assertions.assertDoesNotThrow(() -> guard.assertBranchAccess(BRANCH, "LEADER"));
    }

    @Test
    void assertBranchAccess_whenAnyOfMultipleRequiredRolesMatches_doesNotThrow() {
        UserContextHolder.setBranchRoles(Map.of(BRANCH, Set.of("WAREHOUSE")));

        Assertions.assertDoesNotThrow(() -> guard.assertBranchAccess(BRANCH, "LEADER", "WAREHOUSE"));
    }

    @Test
    void assertBranchAccess_whenBranchNotInTokenKeys_throwsAccessDenied() {
        UserContextHolder.setBranchRoles(Map.of("OTHER_BRANCH", Set.of("LEADER")));

        BusinessException ex = Assertions.assertThrows(BusinessException.class,
                () -> guard.assertBranchAccess(BRANCH, "LEADER"));

        Assertions.assertEquals(ErrorCode.BRANCH_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void assertBranchAccess_whenBranchInTokenButMissingRequiredRole_throwsAccessDenied() {
        // 在 1000 是 SALES、在 1100 是 WAREHOUSE 的人送 branchCode=1000 呼叫配貨（需要 WAREHOUSE）：
        // 「有」該所的權限，只是不是這個操作需要的角色——驗證第 ② 點不可省
        UserContextHolder.setBranchRoles(Map.of(BRANCH, Set.of("SALES"), "OTHER_BRANCH", Set.of("WAREHOUSE")));

        BusinessException ex = Assertions.assertThrows(BusinessException.class,
                () -> guard.assertBranchAccess(BRANCH, "WAREHOUSE"));

        Assertions.assertEquals(ErrorCode.BRANCH_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void assertBranchAccess_whenAdminInAnyOtherBranch_doesNotThrow() {
        UserContextHolder.setBranchRoles(Map.of("OTHER_BRANCH", Set.of("ADMIN")));

        Assertions.assertDoesNotThrow(() -> guard.assertBranchAccess(BRANCH, "LEADER"));
        Mockito.verifyNoInteractions(locationRepo);
    }

    @Test
    void assertLocationOwnership_whenOwnedByCurrentUser_doesNotThrow() {
        UserContextHolder.setUserCode(OPERATOR);
        Location location = new Location();
        location.setLocationCode(LOCATION);
        location.setUserCode(OPERATOR);
        Mockito.when(locationRepo.findByLocationCode(LOCATION)).thenReturn(Optional.of(location));

        Assertions.assertDoesNotThrow(() -> guard.assertLocationOwnership(LOCATION));
    }

    @Test
    void assertLocationOwnership_whenOwnedByAnotherUser_throwsAccessDenied() {
        UserContextHolder.setUserCode(OPERATOR);
        Location location = new Location();
        location.setLocationCode(LOCATION);
        location.setUserCode("OTHER_USER");
        Mockito.when(locationRepo.findByLocationCode(LOCATION)).thenReturn(Optional.of(location));

        BusinessException ex = Assertions.assertThrows(BusinessException.class,
                () -> guard.assertLocationOwnership(LOCATION));

        Assertions.assertEquals(ErrorCode.LOCATION_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void assertLocationOwnership_whenLocationNotFound_throwsResourceNotFound() {
        UserContextHolder.setUserCode(OPERATOR);
        Mockito.when(locationRepo.findByLocationCode(LOCATION)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> guard.assertLocationOwnership(LOCATION));
    }

    @Test
    void assertLocationOwnership_whenAdminInAnyBranch_doesNotThrowAndSkipsLookup() {
        UserContextHolder.setUserCode(OPERATOR);
        UserContextHolder.setBranchRoles(Map.of("SOME_BRANCH", Set.of("ADMIN")));

        Assertions.assertDoesNotThrow(() -> guard.assertLocationOwnership(LOCATION));
        Mockito.verifyNoInteractions(locationRepo);
    }
}
