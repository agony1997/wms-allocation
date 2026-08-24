package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.branch.BranchCreateRequest;
import com.agony.wmsallocation.dto.branch.BranchDto;
import com.agony.wmsallocation.dto.branch.BranchUpdateRequest;
import com.agony.wmsallocation.entity.branch.Branch;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import com.agony.wmsallocation.exception.DuplicateResourceException;
import com.agony.wmsallocation.exception.ResourceInUseException;
import com.agony.wmsallocation.exception.ResourceNotFoundException;
import com.agony.wmsallocation.mapper.BranchMapper;
import com.agony.wmsallocation.repository.AuthUserRepo;
import com.agony.wmsallocation.repository.BranchRepo;
import com.agony.wmsallocation.repository.LocationRepo;
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
class BranchServiceTest {

    @Mock
    private BranchRepo branchRepo;

    @Mock
    private BranchMapper branchMapper;

    @Mock
    private LocationRepo locationRepo;

    @Mock
    private AuthUserRepo authUserRepo;

    @InjectMocks
    private BranchService branchService;

    private Branch stubBranch(String code, ActiveStatus status) {
        Branch branch = new Branch();
        branch.setBranchCode(code);
        branch.setSalesOrgCode("S001");
        branch.setBranchName("測試營業所");
        branch.setStatus(status);
        return branch;
    }

    private BranchDto stubDto(String code, ActiveStatus status) {
        return BranchDto.builder()
                .branchCode(code)
                .salesOrgCode("S001")
                .branchName("測試營業所")
                .status(status)
                .build();
    }

    @Test
    @DisplayName("findAll - 應回傳所有營業所")
    void findAll_shouldReturnAllBranches() {
        Branch branch = stubBranch("B001", ActiveStatus.ACTIVE);
        BranchDto dto = stubDto("B001", ActiveStatus.ACTIVE);
        when(branchRepo.findAll()).thenReturn(List.of(branch));
        when(branchMapper.toDto(branch)).thenReturn(dto);

        List<BranchDto> result = branchService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBranchCode()).isEqualTo("B001");
    }

    @Test
    @DisplayName("findAll 無資料 - 應回傳空清單")
    void findAll_whenEmpty_shouldReturnEmptyList() {
        when(branchRepo.findAll()).thenReturn(List.of());

        assertThat(branchService.findAll()).isEmpty();
    }

    @Test
    @DisplayName("findAllActive - 應以 ACTIVE 狀態查詢")
    void findAllActive_shouldQueryWithActiveStatus() {
        Branch branch = stubBranch("B001", ActiveStatus.ACTIVE);
        BranchDto dto = stubDto("B001", ActiveStatus.ACTIVE);
        when(branchRepo.findByStatus(ActiveStatus.ACTIVE)).thenReturn(List.of(branch));
        when(branchMapper.toDto(branch)).thenReturn(dto);

        List<BranchDto> result = branchService.findAllActive();

        assertThat(result).hasSize(1);
        verify(branchRepo).findByStatus(ActiveStatus.ACTIVE);
    }

    @Test
    @DisplayName("findByBranchCode 存在 - 應回傳 DTO")
    void findByBranchCode_whenExists_shouldReturnDto() {
        Branch branch = stubBranch("B001", ActiveStatus.ACTIVE);
        BranchDto dto = stubDto("B001", ActiveStatus.ACTIVE);
        when(branchRepo.findByBranchCode("B001")).thenReturn(Optional.of(branch));
        when(branchMapper.toDto(branch)).thenReturn(dto);

        BranchDto result = branchService.findByBranchCode("B001");

        assertThat(result).isNotNull();
        assertThat(result.getBranchCode()).isEqualTo("B001");
    }

    @Test
    @DisplayName("findByBranchCode 不存在 - 應拋出 ResourceNotFoundException")
    void findByBranchCode_whenNotExists_shouldThrowResourceNotFound() {
        when(branchRepo.findByBranchCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> branchService.findByBranchCode("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    @DisplayName("create - 應預設 ACTIVE 並存檔回傳 DTO")
    void create_shouldSaveWithActiveStatusAndReturnDto() {
        BranchCreateRequest request = new BranchCreateRequest("B001", "S001", "台北營業所", "台北市", "02-1234");
        Branch saved = stubBranch("B001", ActiveStatus.ACTIVE);
        BranchDto dto = stubDto("B001", ActiveStatus.ACTIVE);
        when(branchRepo.existsByBranchCode("B001")).thenReturn(false);
        when(branchRepo.save(any(Branch.class))).thenReturn(saved);
        when(branchMapper.toDto(saved)).thenReturn(dto);

        BranchDto result = branchService.create(request);

        assertThat(result.getBranchCode()).isEqualTo("B001");
        ArgumentCaptor<Branch> captor = ArgumentCaptor.forClass(Branch.class);
        verify(branchRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ActiveStatus.ACTIVE);
    }

    @Test
    @DisplayName("create 代碼重複 - 應拋出 DuplicateResourceException 且不存檔")
    void create_whenCodeDuplicated_shouldThrowAndNotSave() {
        BranchCreateRequest request = new BranchCreateRequest("B001", "S001", "台北營業所", null, null);
        when(branchRepo.existsByBranchCode("B001")).thenReturn(true);

        assertThatThrownBy(() -> branchService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("B001");
        verify(branchRepo, never()).save(any());
    }

    @Test
    @DisplayName("update - 應更新欄位並存檔，branchCode 不變")
    void update_shouldModifyFieldsAndSave() {
        Branch existing = stubBranch("B001", ActiveStatus.ACTIVE);
        BranchUpdateRequest request = new BranchUpdateRequest("S002", "新北營業所", "新北市", "02-9999");
        when(branchRepo.findByBranchCode("B001")).thenReturn(Optional.of(existing));
        when(branchRepo.save(existing)).thenReturn(existing);
        when(branchMapper.toDto(existing)).thenReturn(stubDto("B001", ActiveStatus.ACTIVE));

        branchService.update("B001", request);

        ArgumentCaptor<Branch> captor = ArgumentCaptor.forClass(Branch.class);
        verify(branchRepo).save(captor.capture());
        Branch saved = captor.getValue();
        assertThat(saved.getBranchCode()).isEqualTo("B001");          // 身份不變
        assertThat(saved.getBranchName()).isEqualTo("新北營業所");      // 欄位已更新
        assertThat(saved.getSalesOrgCode()).isEqualTo("S002");
    }

    @Test
    @DisplayName("update 不存在 - 應拋出 ResourceNotFoundException 且不存檔")
    void update_whenNotExists_shouldThrowResourceNotFound() {
        BranchUpdateRequest request = new BranchUpdateRequest("S002", "新北營業所", null, null);
        when(branchRepo.findByBranchCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> branchService.update("UNKNOWN", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
        verify(branchRepo, never()).save(any());
    }

    @Test
    @DisplayName("delete - 應刪除存在的營業所")
    void delete_shouldRemoveBranch() {
        Branch existing = stubBranch("B001", ActiveStatus.ACTIVE);
        when(branchRepo.findByBranchCode("B001")).thenReturn(Optional.of(existing));

        branchService.delete("B001");

        verify(branchRepo).delete(existing);
    }

    @Test
    @DisplayName("delete 不存在 - 應拋出 ResourceNotFoundException 且不刪除")
    void delete_whenNotExists_shouldThrowResourceNotFound() {
        when(branchRepo.findByBranchCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> branchService.delete("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
        verify(branchRepo, never()).delete(any());
    }

    @Test
    @DisplayName("delete 有儲位下轄 - 應拋出 ResourceInUseException 且不刪除")
    void delete_whenHasDependents_shouldThrowAndNotDelete() {
        Branch existing = stubBranch("B001", ActiveStatus.ACTIVE);
        when(branchRepo.findByBranchCode("B001")).thenReturn(Optional.of(existing));
        when(locationRepo.existsByBranchCode("B001")).thenReturn(true);

        assertThatThrownBy(() -> branchService.delete("B001"))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("B001");
        verify(branchRepo, never()).delete(any());
    }

    @Test
    @DisplayName("delete 有人員下轄 - 應拋出 ResourceInUseException 且不刪除")
    void delete_whenHasAuthUserDependents_shouldThrowAndNotDelete() {
        Branch existing = stubBranch("B001", ActiveStatus.ACTIVE);
        when(branchRepo.findByBranchCode("B001")).thenReturn(Optional.of(existing));
        when(locationRepo.existsByBranchCode("B001")).thenReturn(false);
        when(authUserRepo.existsByBranchCode("B001")).thenReturn(true);

        assertThatThrownBy(() -> branchService.delete("B001"))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("B001");
        verify(branchRepo, never()).delete(any());
    }
}
