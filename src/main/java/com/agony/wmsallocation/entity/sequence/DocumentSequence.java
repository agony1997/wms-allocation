package com.agony.wmsallocation.entity.sequence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 單據取號表：為各類單據產生「{前綴}-{yyyyMMdd}-{3位序號}」格式的唯一編號。
 *
 * <p>每種類型每日獨立計號、序號每日重置從 1 開始；取號流程以悲觀鎖
 * （{@code @Lock(PESSIMISTIC_WRITE)}）在獨立事務（{@code REQUIRES_NEW}）內執行，
 * 序列化同一列存取以避免重號。併發控制決策見
 * {@code docs/adr/0006-sequence-single-pessimistic-lock.md}，業務規則詳見
 * {@code docs/requirements/specification/SequenceNumber.md}。
 * <p>複合主鍵：(sequenceType, sequenceDate)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document_sequence")
@IdClass(DocumentSequenceId.class)
public class DocumentSequence {

    @Id
    @Column(name = "sequence_type", length = 10, nullable = false)
    private String sequenceType;

    @Id
    @Column(name = "sequence_date", nullable = false)
    private LocalDate sequenceDate;

    /** 該類型+日期當前已發出的最大序號；下次取號為此值 +1。 */
    @Column(name = "current_no", nullable = false)
    private int currentNo;
}
