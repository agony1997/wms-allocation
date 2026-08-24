package com.agony.wmsallocation.service;

import com.agony.wmsallocation.entity.sequence.DocumentSequence;
import com.agony.wmsallocation.entity.sequence.enums.SequenceType;
import com.agony.wmsallocation.exception.SequenceOverflowException;
import com.agony.wmsallocation.repository.SequenceRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// Todo 加其他實作時再抽介面
@RequiredArgsConstructor
@Service
public class SequenceService {

    private final SequenceRepo sequenceRepo;

    // 避免呼叫端交易回滾把已發出的序號一起吸回去，導致重號
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateSequence(SequenceType sequenceType, LocalDate sequenceDate) {

        DocumentSequence documentSequence = sequenceRepo.findBySequenceTypeAndSequenceDate(sequenceType.getCode(), sequenceDate)
                .map(sequence -> {
                    sequence.setCurrentNo(sequence.getCurrentNo() + 1);
                    return sequence;
                })
                .orElseGet(() -> new DocumentSequence(sequenceType.getCode(), sequenceDate, 1));
        // ponytail: 冷啟動缺口——當日首筆並發可能同時 insert 撞複合主鍵。SQL Server 的
        // HOLDLOCK key-range lock 大致已擋住；暫不處理，等多執行緒整合測試能重現時再補
        // 「捕 PK 衝突→重讀」（非預建列）。決策見 ADR-0008、規格見 SequenceNumber.md。

        if (documentSequence.getCurrentNo() > 999) {
            throw new SequenceOverflowException(
                    String.format("%s 於 %s 當日序號已達上限 999，無法再取號",
                            sequenceType.getCode(),
                            sequenceDate.format(DateTimeFormatter.BASIC_ISO_DATE)));
        }

        sequenceRepo.save(documentSequence);

        return this.assembleNo(sequenceType.getCode(), sequenceDate, documentSequence.getCurrentNo());
    }

    private String assembleNo(String sequenceType, LocalDate sequenceDate, int no) {
        return String.format("%s-%s-%03d",
                sequenceType,
                sequenceDate.format(DateTimeFormatter.BASIC_ISO_DATE),
                no);
    }
}
