package com.agony.wmsallocation.service;

import com.agony.wmsallocation.entity.sequence.DocumentSequence;
import com.agony.wmsallocation.entity.sequence.enums.SequenceType;
import com.agony.wmsallocation.exception.SequenceOverflowException;
import com.agony.wmsallocation.repository.SequenceRepo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class SequenceServiceTest {

    @Mock
    private SequenceRepo sequenceRepo;

    @InjectMocks
    private SequenceService sequenceService;

    @Test
    public void generateSequence_whenFirstOfDay() {
        // Arrange
        LocalDate date = LocalDate.of(2020, 1, 1);
        Mockito.when(sequenceRepo.findBySequenceTypeAndSequenceDate(SequenceType.SPO.getCode(), date))
                .thenReturn(Optional.empty());

        // Act
        String resultNo = sequenceService.generateSequence(SequenceType.SPO, date);
        // Assert
        Assertions.assertEquals(SequenceType.SPO.getCode() + "-20200101-001", resultNo);

        ArgumentCaptor<DocumentSequence> captor = ArgumentCaptor.forClass(DocumentSequence.class);
        Mockito.verify(sequenceRepo).save(captor.capture());
        Assertions.assertEquals(1, captor.getValue().getCurrentNo());
    }

    @Test
    public void generateSequence_whenExceeds999_throwsAndDoesNotSave() {
        // Arrange
        LocalDate date = LocalDate.of(2020, 1, 1);
        DocumentSequence maxed = new DocumentSequence(SequenceType.SPO.getCode(), date, 999);
        Mockito.when(sequenceRepo.findBySequenceTypeAndSequenceDate(SequenceType.SPO.getCode(), date))
                .thenReturn(Optional.of(maxed));

        // Act & Assert
        Assertions.assertThrows(SequenceOverflowException.class,
                () -> sequenceService.generateSequence(SequenceType.SPO, date));

        // 溢號時不可把 1000 存回去
        Mockito.verify(sequenceRepo, Mockito.never()).save(Mockito.any());
    }

}
