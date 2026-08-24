package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.master.SalesOrganizationCreateRequest;
import com.agony.wmsallocation.dto.master.SalesOrganizationDto;
import com.agony.wmsallocation.dto.master.SalesOrganizationUpdateRequest;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import com.agony.wmsallocation.entity.master.SalesOrganization;
import com.agony.wmsallocation.exception.DuplicateResourceException;
import com.agony.wmsallocation.exception.ResourceInUseException;
import com.agony.wmsallocation.exception.ResourceNotFoundException;
import com.agony.wmsallocation.mapper.SalesOrganizationMapper;
import com.agony.wmsallocation.repository.BranchRepo;
import com.agony.wmsallocation.repository.CustomerRepo;
import com.agony.wmsallocation.repository.SalesOrganizationRepo;
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
class SalesOrganizationServiceTest {

    @Mock
    private SalesOrganizationRepo salesOrganizationRepo;

    @Mock
    private SalesOrganizationMapper salesOrganizationMapper;

    @Mock
    private BranchRepo branchRepo;

    @Mock
    private CustomerRepo customerRepo;

    @InjectMocks
    private SalesOrganizationService salesOrganizationService;

    private SalesOrganization stubEntity(String code, ActiveStatus status) {
        SalesOrganization salesOrg = new SalesOrganization();
        salesOrg.setSalesOrgCode(code);
        salesOrg.setSalesOrgName("測試銷售組織");
        salesOrg.setStatus(status);
        return salesOrg;
    }

    private SalesOrganizationDto stubDto(String code, ActiveStatus status) {
        return SalesOrganizationDto.builder()
                .salesOrgCode(code)
                .salesOrgName("測試銷售組織")
                .status(status)
                .build();
    }

    @Test
    @DisplayName("findAll - 應回傳所有銷售組織")
    void findAll_shouldReturnAll() {
        SalesOrganization entity = stubEntity("S001", ActiveStatus.ACTIVE);
        when(salesOrganizationRepo.findAll()).thenReturn(List.of(entity));
        when(salesOrganizationMapper.toDto(entity)).thenReturn(stubDto("S001", ActiveStatus.ACTIVE));

        List<SalesOrganizationDto> result = salesOrganizationService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSalesOrgCode()).isEqualTo("S001");
    }

    @Test
    @DisplayName("findAll 無資料 - 應回傳空清單")
    void findAll_whenEmpty_shouldReturnEmptyList() {
        when(salesOrganizationRepo.findAll()).thenReturn(List.of());

        assertThat(salesOrganizationService.findAll()).isEmpty();
    }

    @Test
    @DisplayName("findAllActive - 應以 ACTIVE 狀態查詢")
    void findAllActive_shouldQueryWithActiveStatus() {
        SalesOrganization entity = stubEntity("S001", ActiveStatus.ACTIVE);
        when(salesOrganizationRepo.findByStatus(ActiveStatus.ACTIVE)).thenReturn(List.of(entity));
        when(salesOrganizationMapper.toDto(entity)).thenReturn(stubDto("S001", ActiveStatus.ACTIVE));

        List<SalesOrganizationDto> result = salesOrganizationService.findAllActive();

        assertThat(result).hasSize(1);
        verify(salesOrganizationRepo).findByStatus(ActiveStatus.ACTIVE);
    }

    @Test
    @DisplayName("findBySalesOrgCode 存在 - 應回傳 DTO")
    void findBySalesOrgCode_whenExists_shouldReturnDto() {
        SalesOrganization entity = stubEntity("S001", ActiveStatus.ACTIVE);
        when(salesOrganizationRepo.findBySalesOrgCode("S001")).thenReturn(Optional.of(entity));
        when(salesOrganizationMapper.toDto(entity)).thenReturn(stubDto("S001", ActiveStatus.ACTIVE));

        SalesOrganizationDto result = salesOrganizationService.findBySalesOrgCode("S001");

        assertThat(result.getSalesOrgCode()).isEqualTo("S001");
    }

    @Test
    @DisplayName("findBySalesOrgCode 不存在 - 應拋出 ResourceNotFoundException")
    void findBySalesOrgCode_whenNotExists_shouldThrowResourceNotFound() {
        when(salesOrganizationRepo.findBySalesOrgCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salesOrganizationService.findBySalesOrgCode("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    @DisplayName("create - 應預設 ACTIVE 並存檔回傳 DTO")
    void create_shouldSaveWithActiveStatusAndReturnDto() {
        SalesOrganizationCreateRequest request = new SalesOrganizationCreateRequest("S001", "北區銷售組織");
        SalesOrganization saved = stubEntity("S001", ActiveStatus.ACTIVE);
        when(salesOrganizationRepo.existsBySalesOrgCode("S001")).thenReturn(false);
        when(salesOrganizationRepo.save(any(SalesOrganization.class))).thenReturn(saved);
        when(salesOrganizationMapper.toDto(saved)).thenReturn(stubDto("S001", ActiveStatus.ACTIVE));

        SalesOrganizationDto result = salesOrganizationService.create(request);

        assertThat(result.getSalesOrgCode()).isEqualTo("S001");
        ArgumentCaptor<SalesOrganization> captor = ArgumentCaptor.forClass(SalesOrganization.class);
        verify(salesOrganizationRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ActiveStatus.ACTIVE);
        assertThat(captor.getValue().getSalesOrgName()).isEqualTo("北區銷售組織");
    }

    @Test
    @DisplayName("create 代碼重複 - 應拋出 DuplicateResourceException 且不存檔")
    void create_whenCodeDuplicated_shouldThrowAndNotSave() {
        SalesOrganizationCreateRequest request = new SalesOrganizationCreateRequest("S001", "北區銷售組織");
        when(salesOrganizationRepo.existsBySalesOrgCode("S001")).thenReturn(true);

        assertThatThrownBy(() -> salesOrganizationService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("S001");
        verify(salesOrganizationRepo, never()).save(any());
    }

    @Test
    @DisplayName("update - 應更新名稱並存檔，salesOrgCode 不變")
    void update_shouldModifyNameAndSave() {
        SalesOrganization existing = stubEntity("S001", ActiveStatus.ACTIVE);
        SalesOrganizationUpdateRequest request = new SalesOrganizationUpdateRequest("南區銷售組織");
        when(salesOrganizationRepo.findBySalesOrgCode("S001")).thenReturn(Optional.of(existing));
        when(salesOrganizationRepo.save(existing)).thenReturn(existing);
        when(salesOrganizationMapper.toDto(existing)).thenReturn(stubDto("S001", ActiveStatus.ACTIVE));

        salesOrganizationService.update("S001", request);

        ArgumentCaptor<SalesOrganization> captor = ArgumentCaptor.forClass(SalesOrganization.class);
        verify(salesOrganizationRepo).save(captor.capture());
        assertThat(captor.getValue().getSalesOrgCode()).isEqualTo("S001");        // 身份不變
        assertThat(captor.getValue().getSalesOrgName()).isEqualTo("南區銷售組織");  // 欄位已更新
    }

    @Test
    @DisplayName("update 不存在 - 應拋出 ResourceNotFoundException 且不存檔")
    void update_whenNotExists_shouldThrowResourceNotFound() {
        SalesOrganizationUpdateRequest request = new SalesOrganizationUpdateRequest("南區銷售組織");
        when(salesOrganizationRepo.findBySalesOrgCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salesOrganizationService.update("UNKNOWN", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
        verify(salesOrganizationRepo, never()).save(any());
    }

    @Test
    @DisplayName("delete 無下轄 - 應刪除")
    void delete_whenNoDependents_shouldRemove() {
        SalesOrganization existing = stubEntity("S001", ActiveStatus.ACTIVE);
        when(salesOrganizationRepo.findBySalesOrgCode("S001")).thenReturn(Optional.of(existing));
        // branchRepo / customerRepo 的 existsBySalesOrgCode 未 stub，預設回 false → 無下轄

        salesOrganizationService.delete("S001");

        verify(salesOrganizationRepo).delete(existing);
    }

    @Test
    @DisplayName("delete 不存在 - 應拋出 ResourceNotFoundException 且不刪除")
    void delete_whenNotExists_shouldThrowResourceNotFound() {
        when(salesOrganizationRepo.findBySalesOrgCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salesOrganizationService.delete("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
        verify(salesOrganizationRepo, never()).delete(any());
    }

    @Test
    @DisplayName("delete 有營業所下轄 - 應拋出 ResourceInUseException 且不刪除")
    void delete_whenHasBranchDependents_shouldThrowAndNotDelete() {
        SalesOrganization existing = stubEntity("S001", ActiveStatus.ACTIVE);
        when(salesOrganizationRepo.findBySalesOrgCode("S001")).thenReturn(Optional.of(existing));
        when(branchRepo.existsBySalesOrgCode("S001")).thenReturn(true);

        assertThatThrownBy(() -> salesOrganizationService.delete("S001"))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("S001");
        verify(salesOrganizationRepo, never()).delete(any());
    }

    @Test
    @DisplayName("delete 有客戶下轄 - 應拋出 ResourceInUseException 且不刪除")
    void delete_whenHasCustomerDependents_shouldThrowAndNotDelete() {
        SalesOrganization existing = stubEntity("S001", ActiveStatus.ACTIVE);
        when(salesOrganizationRepo.findBySalesOrgCode("S001")).thenReturn(Optional.of(existing));
        when(branchRepo.existsBySalesOrgCode("S001")).thenReturn(false);
        when(customerRepo.existsBySalesOrgCode("S001")).thenReturn(true);

        assertThatThrownBy(() -> salesOrganizationService.delete("S001"))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("S001");
        verify(salesOrganizationRepo, never()).delete(any());
    }
}
