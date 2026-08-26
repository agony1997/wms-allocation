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

    /** locationCode 全域唯一，單鍵即可定位；branchCode 由本方法反查取得，不由呼叫端指定。 */
    Optional<Location> findByLocationCode(String locationCode);

    List<Location> findByStatus(ActiveStatus status);

    List<Location> findByBranchCodeAndStatus(String branchCode, ActiveStatus status);

}
