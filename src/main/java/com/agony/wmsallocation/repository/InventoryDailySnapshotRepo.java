package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.inventory.InventoryDailySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InventoryDailySnapshotRepo extends JpaRepository<InventoryDailySnapshot, Integer> {

    /**
     * 查詢某天的所有快照
     */
    List<InventoryDailySnapshot> findBySnapshotDate(LocalDate snapshotDate);

    /**
     * 查詢某天某營業所的大庫快照
     */
    List<InventoryDailySnapshot> findBySnapshotDateAndBranchCode(LocalDate snapshotDate, String branchCode);

    /**
     * 刪除某天的所有快照（重新產生時用）
     */
    void deleteBySnapshotDate(LocalDate snapshotDate);

    /**
     * 檢查某天是否已有快照
     */
    boolean existsBySnapshotDate(LocalDate snapshotDate);

}
