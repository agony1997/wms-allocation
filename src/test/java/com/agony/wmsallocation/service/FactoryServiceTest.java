package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.master.FactoryCreateRequest;
import com.agony.wmsallocation.dto.master.FactoryDto;
import com.agony.wmsallocation.dto.master.FactoryUpdateRequest;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import com.agony.wmsallocation.entity.master.Factory;
import com.agony.wmsallocation.exception.DuplicateResourceException;
import com.agony.wmsallocation.exception.ResourceNotFoundException;
import com.agony.wmsallocation.mapper.FactoryMapper;
import com.agony.wmsallocation.repository.FactoryRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FactoryServiceTest {

    @Mock
    private FactoryRepo factoryRepo;

    @Mock
    private FactoryMapper factoryMapper;

    @InjectMocks
    private FactoryService factoryService;

    private Factory stubEntity(String code, ActiveStatus status) {
        Factory factory = new Factory();
        factory.setFactoryCode(code);
        factory.setFactoryName("測試工廠");
        factory.setStatus(status);
        return factory;
    }

    private FactoryDto stubDto(String code, ActiveStatus status) {
        return FactoryDto.builder()
                .factoryCode(code)
                .factoryName("測試工廠")
                .status(status)
                .build();
    }

    @Test
    @DisplayName("findAll - 應回傳所有工廠")
    void findAll_shouldReturnAll() {
        Factory entity = stubEntity("F001", ActiveStatus.ACTIVE);
        when(factoryRepo.findAll()).thenReturn(List.of(entity));
        when(factoryMapper.toDto(entity)).thenReturn(stubDto("F001", ActiveStatus.ACTIVE));

        List<FactoryDto> result = factoryService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFactoryCode()).isEqualTo("F001");
    }

    @Test
    @DisplayName("findAll 無資料 - 應回傳空清單")
    void findAll_whenEmpty_shouldReturnEmptyList() {
        when(factoryRepo.findAll()).thenReturn(List.of());

        assertThat(factoryService.findAll()).isEmpty();
    }

    @Test
    @DisplayName("findAllActive - 應以 ACTIVE 狀態查詢")
    void findAllActive_shouldQueryWithActiveStatus() {
        Factory entity = stubEntity("F001", ActiveStatus.ACTIVE);
        when(factoryRepo.findByStatus(ActiveStatus.ACTIVE)).thenReturn(List.of(entity));
        when(factoryMapper.toDto(entity)).thenReturn(stubDto("F001", ActiveStatus.ACTIVE));

        List<FactoryDto> result = factoryService.findAllActive();

        assertThat(result).hasSize(1);
        verify(factoryRepo).findByStatus(ActiveStatus.ACTIVE);
    }

    @Test
    @DisplayName("findByFactoryCode 存在 - 應回傳 DTO")
    void findByFactoryCode_whenExists_shouldReturnDto() {
        Factory entity = stubEntity("F001", ActiveStatus.ACTIVE);
        when(factoryRepo.findByFactoryCode("F001")).thenReturn(Optional.of(entity));
        when(factoryMapper.toDto(entity)).thenReturn(stubDto("F001", ActiveStatus.ACTIVE));

        FactoryDto result = factoryService.findByFactoryCode("F001");

        assertThat(result.getFactoryCode()).isEqualTo("F001");
    }

    @Test
    @DisplayName("findByFactoryCode 不存在 - 應拋出 ResourceNotFoundException")
    void findByFactoryCode_whenNotExists_shouldThrowResourceNotFound() {
        when(factoryRepo.findByFactoryCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> factoryService.findByFactoryCode("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    @DisplayName("create - 應預設 ACTIVE 並存檔回傳 DTO")
    void create_shouldSaveWithActiveStatusAndReturnDto() {
        FactoryCreateRequest request = new FactoryCreateRequest("F001", "台北工廠", "台北市", "02-1234");
        Factory saved = stubEntity("F001", ActiveStatus.ACTIVE);
        when(factoryRepo.existsByFactoryCode("F001")).thenReturn(false);
        when(factoryRepo.save(any(Factory.class))).thenReturn(saved);
        when(factoryMapper.toDto(saved)).thenReturn(stubDto("F001", ActiveStatus.ACTIVE));

        FactoryDto result = factoryService.create(request);

        assertThat(result.getFactoryCode()).isEqualTo("F001");
        ArgumentCaptor<Factory> captor = ArgumentCaptor.forClass(Factory.class);
        verify(factoryRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ActiveStatus.ACTIVE);
        assertThat(captor.getValue().getFactoryName()).isEqualTo("台北工廠");
    }

    @Test
    @DisplayName("create 代碼重複 - 應拋出 DuplicateResourceException 且不存檔")
    void create_whenCodeDuplicated_shouldThrowAndNotSave() {
        FactoryCreateRequest request = new FactoryCreateRequest("F001", "台北工廠", null, null);
        when(factoryRepo.existsByFactoryCode("F001")).thenReturn(true);

        assertThatThrownBy(() -> factoryService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("F001");
        verify(factoryRepo, never()).save(any());
    }

    @Test
    @DisplayName("update - 應更新欄位並存檔，factoryCode 不變")
    void update_shouldModifyFieldsAndSave() {
        Factory existing = stubEntity("F001", ActiveStatus.ACTIVE);
        FactoryUpdateRequest request = new FactoryUpdateRequest("桃園工廠", "桃園市", "03-9999");
        when(factoryRepo.findByFactoryCode("F001")).thenReturn(Optional.of(existing));
        when(factoryRepo.save(existing)).thenReturn(existing);
        when(factoryMapper.toDto(existing)).thenReturn(stubDto("F001", ActiveStatus.ACTIVE));

        factoryService.update("F001", request);

        ArgumentCaptor<Factory> captor = ArgumentCaptor.forClass(Factory.class);
        verify(factoryRepo).save(captor.capture());
        Factory saved = captor.getValue();
        assertThat(saved.getFactoryCode()).isEqualTo("F001");       // 身份不變
        assertThat(saved.getFactoryName()).isEqualTo("桃園工廠");     // 欄位已更新
        assertThat(saved.getAddress()).isEqualTo("桃園市");
    }

    @Test
    @DisplayName("update 不存在 - 應拋出 ResourceNotFoundException 且不存檔")
    void update_whenNotExists_shouldThrowResourceNotFound() {
        FactoryUpdateRequest request = new FactoryUpdateRequest("桃園工廠", null, null);
        when(factoryRepo.findByFactoryCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> factoryService.update("UNKNOWN", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
        verify(factoryRepo, never()).save(any());
    }

    @Test
    @DisplayName("delete - 應刪除存在的工廠")
    void delete_shouldRemoveFactory() {
        Factory existing = stubEntity("F001", ActiveStatus.ACTIVE);
        when(factoryRepo.findByFactoryCode("F001")).thenReturn(Optional.of(existing));

        factoryService.delete("F001");

        verify(factoryRepo).delete(existing);
    }

    @Test
    @DisplayName("delete 不存在 - 應拋出 ResourceNotFoundException 且不刪除")
    void delete_whenNotExists_shouldThrowResourceNotFound() {
        when(factoryRepo.findByFactoryCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> factoryService.delete("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
        verify(factoryRepo, never()).delete(any());
    }
}
