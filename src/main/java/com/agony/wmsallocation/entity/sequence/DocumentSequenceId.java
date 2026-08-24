package com.agony.wmsallocation.entity.sequence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * {@link DocumentSequence} 的複合主鍵類別（@IdClass）：欄位名與型別須與 Entity 上的
 * 各 @Id 欄位完全對應，JPA 才能正確組成主鍵。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSequenceId implements Serializable {
    private String sequenceType;
    private LocalDate sequenceDate;
}
