package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.sequence.DocumentSequence;
import com.agony.wmsallocation.entity.sequence.DocumentSequenceId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface SequenceRepo extends JpaRepository<DocumentSequence, DocumentSequenceId> {

    // 悲觀鎖鎖住這一列（ADR-0006）；要撐多久由呼叫端 Service 的 @Transactional(REQUIRES_NEW) 決定
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DocumentSequence> findBySequenceTypeAndSequenceDate(String sequenceType, LocalDate sequenceDate);

}
