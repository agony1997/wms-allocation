package com.agony.wmsallocation.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 配貨分配演算法（純函式，不碰 DB）：對「單一產品」的待配需求，依業務員優先度與 FEFO
 * 從大庫批次分配，單批不足時跨批拆分。業務規則見
 * {@code docs/requirements/specification/branch/SalesPriority.md}。
 *
 * <p>抽成獨立無狀態類，讓演算法脫離 Spring/Repo 單元測試（ADR-0005：真實業務規則 test-first）。
 * 配貨 Service 負責 selection（撈待配 SPOD、組 demand/batch）與落庫，本類只算「誰拿哪批多少」。
 */
public class AllocationCalculator {

    /**
     * 一位業務員對某產品的待配需求。priorityLevel 為 null 視為最低優先。
     */
    public record AllocationDemand(String locationCode, Integer priorityLevel, int requestedQty) {
    }

    /**
     * 大庫某批次的可用量。
     */
    public record BatchStock(String batchNo, LocalDate expiryDate, int availableQty) {
    }

    /**
     * 一筆配貨結果（某業務員 × 某批次），最終落成一筆 AOD。
     * requestedQty = 原始預定量（來自 SPOD.confirmedQty；同一需求跨批拆成多筆時每筆相同），
     * allocatedQty = 該批實際分到量。缺口 = requestedQty − 該需求所有筆 allocatedQty 加總。
     */
    public record AllocationLine(String locationCode, String batchNo, LocalDate expiryDate,
                                 int requestedQty, int allocatedQty) {
    }

    /**
     * 分配過程中剩餘量會被扣減的批次（BatchStock 是不可變 record，工作階段用此可變結構）。
     */
    private static final class RemainingBatch {
        final String batchNo;
        final LocalDate expiryDate;
        int remaining;

        RemainingBatch(String batchNo, LocalDate expiryDate, int remaining) {
            this.batchNo = batchNo;
            this.expiryDate = expiryDate;
            this.remaining = remaining;
        }
    }

    /**
     * 對單一產品執行分配。
     *
     * <p>規則：
     * <ul>
     *   <li>demands 依 priorityLevel ASC 依序分配（null 視為最低；同級以 locationCode ASC 決定先後）。</li>
     *   <li>每位業務員依 FEFO（expiryDate ASC, batchNo ASC）跨批取貨，單批不足時拆成多筆 AllocationLine。</li>
     *   <li>完全分不到的業務員不產生 AllocationLine（其 SPOD 仍於 Service 層轉 ALLOCATED，防重複配貨）。</li>
     *   <li>庫存耗盡後尚未輪到的業務員自然拿不到（不自動補配）。</li>
     * </ul>
     *
     * @param demands 待配需求（順序不拘，內部自行排序）
     * @param batches 大庫可用批次（順序不拘，內部自行排序）
     * @return 配貨結果明細；只含實際分到貨（allocatedQty &gt; 0）的筆
     */
    public List<AllocationLine> allocate(List<AllocationDemand> demands, List<BatchStock> batches) {
        // ── 第一階段：排序 + 壓成可扣減的批次結構 ──
        // demands 依優先度排序：priorityLevel ASC（null 最低），同級 locationCode ASC
        List<AllocationDemand> sortedDemands = demands.stream()
                .sorted(Comparator.comparing(AllocationDemand::priorityLevel, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AllocationDemand::locationCode))
                .toList();

        // batches 依 FEFO 排序（expiryDate ASC, batchNo ASC）後壓成剩餘量可扣減的工作結構
        // （BatchStock 是不可變 record，欄位用 accessor 方法 expiryDate()/batchNo() 取）
        List<RemainingBatch> fefoBatches = batches.stream()
                .sorted(Comparator.comparing(BatchStock::expiryDate)
                        .thenComparing(BatchStock::batchNo))
                .map(b -> new RemainingBatch(b.batchNo(), b.expiryDate(), b.availableQty()))
                .toList();

        ArrayList<AllocationLine> allocationLines = new ArrayList<>();

        // ── 第二階段：依優先度序，每人 FEFO 跨批取貨，邊拿邊扣批次餘量與剩餘需求 ──
        for (AllocationDemand demand : sortedDemands) {
            int remainRequestedQty = demand.requestedQty(); // 此人剩多少要拿
            for (RemainingBatch batch : fefoBatches) {
                if (remainRequestedQty == 0) break; // 此人拿滿了
                if (batch.remaining < 1) continue;  // 該批次沒量了, 下一批次

                int takeQty = Math.min(batch.remaining, remainRequestedQty);

                allocationLines.add(new AllocationLine(demand.locationCode(),
                        batch.batchNo,
                        batch.expiryDate,
                        demand.requestedQty(),
                        takeQty));

                remainRequestedQty -= takeQty;
                batch.remaining -= takeQty;
            }
        }

        return allocationLines;
    }

}
