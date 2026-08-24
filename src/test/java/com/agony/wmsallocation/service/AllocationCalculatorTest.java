package com.agony.wmsallocation.service;

import com.agony.wmsallocation.service.AllocationCalculator.AllocationDemand;
import com.agony.wmsallocation.service.AllocationCalculator.AllocationLine;
import com.agony.wmsallocation.service.AllocationCalculator.BatchStock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配貨分配演算法測試（test-first，ADR-0005）。釘住四類行為：
 * FEFO 取批、優先度排序、跨批拆分與 requestedQty 語意、缺貨分 0 不建筆。
 */
class AllocationCalculatorTest {

    private AllocationCalculator calc;

    @BeforeEach
    void setUp() {
        calc = new AllocationCalculator();
    }

    // FEFO：近效期批次先出（輸入故意亂序，驗證演算法內部自行排序）
    @Test
    void fefo_takesNearestExpiryFirst() {
        var demands = List.of(new AllocationDemand("CAR-A", 1, 5));
        var batches = List.of(
                new BatchStock("B1", LocalDate.of(2024, 1, 20), 10),
                new BatchStock("B2", LocalDate.of(2024, 1, 15), 8));   // 近效期是 B2

        var lines = calc.allocate(demands, batches);

        assertEquals(1, lines.size());
        assertEquals("B2", lines.get(0).batchNo());       // 近效期先出
        assertEquals(5, lines.get(0).allocatedQty());
        assertEquals(5, lines.get(0).requestedQty());
    }

    // 優先度：高優先先分；庫存只夠一人時，低優先分到 0 → 不產生 line
    @Test
    void priority_higherFirst_lowerGetsNoLine() {
        var demands = List.of(
                new AllocationDemand("CAR-B", 2, 10),
                new AllocationDemand("CAR-A", 1, 10));                  // 亂序
        var batches = List.of(new BatchStock("B1", LocalDate.of(2024, 1, 15), 10));

        var lines = calc.allocate(demands, batches);

        assertEquals(1, lines.size());                     // 分到 0 的不建 line
        assertEquals("CAR-A", lines.get(0).locationCode());
        assertEquals(10, lines.get(0).allocatedQty());
    }

    // 跨批拆分 + requestedQty = 進場剩餘需求（規格 SalesPriority.md 的範例）
    @Test
    void splitAcrossBatches_requestedIsRemainingOnEntry() {
        var demands = List.of(
                new AllocationDemand("CAR-A", 1, 10),
                new AllocationDemand("CAR-B", 2, 10));
        var batches = List.of(
                new BatchStock("B2", LocalDate.of(2024, 1, 15), 12),   // 近效期
                new BatchStock("B1", LocalDate.of(2024, 1, 20), 5));

        var lines = calc.allocate(demands, batches);

        // A（優先）：B2 × 10
        var aLines = lines.stream().filter(l -> l.locationCode().equals("CAR-A")).toList();
        assertEquals(1, aLines.size());
        assertEquals("B2", aLines.get(0).batchNo());
        assertEquals(10, aLines.get(0).requestedQty());
        assertEquals(10, aLines.get(0).allocatedQty());

        // B：B2 × 2 ＋ B1 × 5；requestedQty 兩筆都記原始需求 10；缺 3 不建筆
        var bLines = lines.stream().filter(l -> l.locationCode().equals("CAR-B"))
                .sorted(Comparator.comparing(AllocationLine::expiryDate)).toList();
        assertEquals(2, bLines.size());
        assertEquals("B2", bLines.get(0).batchNo());
        assertEquals(10, bLines.get(0).requestedQty());
        assertEquals(2, bLines.get(0).allocatedQty());
        assertEquals("B1", bLines.get(1).batchNo());
        assertEquals(10, bLines.get(1).requestedQty());    // 原始需求（每筆 AOD 都記 confirmedQty）
        assertEquals(5, bLines.get(1).allocatedQty());
    }

    // 跨批時 take 要用「剩餘需求」而非原始需求，否則第二批超拿（其他案例批次都比需求小，抓不到）
    @Test
    void splitAcrossBatches_takeUsesRemainingNotOriginal() {
        var demands = List.of(new AllocationDemand("CAR-A", 1, 10));
        var batches = List.of(
                new BatchStock("B1", LocalDate.of(2024, 1, 15), 6),
                new BatchStock("B2", LocalDate.of(2024, 1, 20), 6));   // 兩批各 6，都大於拿完第一批後的剩餘 4

        var lines = calc.allocate(demands, batches);

        assertEquals(2, lines.size());
        var sorted = lines.stream().sorted(Comparator.comparing(AllocationLine::expiryDate)).toList();
        assertEquals(6, sorted.get(0).allocatedQty());     // B1 拿 6 → 剩餘需求 4
        assertEquals(4, sorted.get(1).allocatedQty());     // B2 只能拿 4（不是 6，不超發）
    }

    // null 優先度視為最低；同級以 locationCode ASC 決定先後（庫存不足以區分順序）
    @Test
    void nullPriorityLast_sameLevelByLocationAsc() {
        var demands = List.of(
                new AllocationDemand("CAR-C", null, 3),                // null → 最低
                new AllocationDemand("CAR-B", 1, 3),
                new AllocationDemand("CAR-A", 1, 3));                  // 同級，locationCode ASC → A 先
        var batches = List.of(new BatchStock("B1", LocalDate.of(2024, 1, 15), 5));  // 只夠 5

        var lines = calc.allocate(demands, batches);

        // 順序：CAR-A 拿滿 3 → CAR-B 只剩 2 → CAR-C（null 最低）分到 0 不建筆
        assertEquals(2, lines.size());
        assertEquals(3, lineOf(lines, "CAR-A").allocatedQty());
        assertEquals(2, lineOf(lines, "CAR-B").allocatedQty());
        assertTrue(lines.stream().noneMatch(l -> l.locationCode().equals("CAR-C")));
    }

    private AllocationLine lineOf(List<AllocationLine> lines, String locationCode) {
        return lines.stream().filter(l -> l.locationCode().equals(locationCode)).findFirst().orElseThrow();
    }
}
