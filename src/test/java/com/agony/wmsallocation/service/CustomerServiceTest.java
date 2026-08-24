package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.master.CustomerCreateRequest;
import com.agony.wmsallocation.dto.master.CustomerDto;
import com.agony.wmsallocation.dto.master.CustomerUpdateRequest;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import com.agony.wmsallocation.entity.master.Customer;
import com.agony.wmsallocation.exception.DuplicateResourceException;
import com.agony.wmsallocation.exception.ResourceNotFoundException;
import com.agony.wmsallocation.mapper.CustomerMapper;
import com.agony.wmsallocation.repository.CustomerRepo;
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
class CustomerServiceTest {

    @Mock
    private CustomerRepo customerRepo;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerService customerService;

    private Customer stubEntity(String code, ActiveStatus status) {
        Customer customer = new Customer();
        customer.setCustomerCode(code);
        customer.setCustomerName("測試客戶");
        customer.setSalesOrgCode("S001");
        customer.setUserCode("U001");
        customer.setStatus(status);
        return customer;
    }

    private CustomerDto stubDto(String code, ActiveStatus status) {
        return CustomerDto.builder()
                .customerCode(code)
                .customerName("測試客戶")
                .salesOrgCode("S001")
                .userCode("U001")
                .status(status)
                .build();
    }

    @Test
    @DisplayName("findAll - 應回傳所有客戶")
    void findAll_shouldReturnAll() {
        Customer entity = stubEntity("C001", ActiveStatus.ACTIVE);
        when(customerRepo.findAll()).thenReturn(List.of(entity));
        when(customerMapper.toDto(entity)).thenReturn(stubDto("C001", ActiveStatus.ACTIVE));

        List<CustomerDto> result = customerService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerCode()).isEqualTo("C001");
    }

    @Test
    @DisplayName("findAll 無資料 - 應回傳空清單")
    void findAll_whenEmpty_shouldReturnEmptyList() {
        when(customerRepo.findAll()).thenReturn(List.of());

        assertThat(customerService.findAll()).isEmpty();
    }

    @Test
    @DisplayName("findAllActive - 應以 ACTIVE 狀態查詢")
    void findAllActive_shouldQueryWithActiveStatus() {
        Customer entity = stubEntity("C001", ActiveStatus.ACTIVE);
        when(customerRepo.findByStatus(ActiveStatus.ACTIVE)).thenReturn(List.of(entity));
        when(customerMapper.toDto(entity)).thenReturn(stubDto("C001", ActiveStatus.ACTIVE));

        List<CustomerDto> result = customerService.findAllActive();

        assertThat(result).hasSize(1);
        verify(customerRepo).findByStatus(ActiveStatus.ACTIVE);
    }

    @Test
    @DisplayName("findByCustomerCode 存在 - 應回傳 DTO")
    void findByCustomerCode_whenExists_shouldReturnDto() {
        Customer entity = stubEntity("C001", ActiveStatus.ACTIVE);
        when(customerRepo.findByCustomerCode("C001")).thenReturn(Optional.of(entity));
        when(customerMapper.toDto(entity)).thenReturn(stubDto("C001", ActiveStatus.ACTIVE));

        CustomerDto result = customerService.findByCustomerCode("C001");

        assertThat(result.getCustomerCode()).isEqualTo("C001");
    }

    @Test
    @DisplayName("findByCustomerCode 不存在 - 應拋出 ResourceNotFoundException")
    void findByCustomerCode_whenNotExists_shouldThrowResourceNotFound() {
        when(customerRepo.findByCustomerCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findByCustomerCode("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    @DisplayName("create - 應預設 ACTIVE 並存檔回傳 DTO")
    void create_shouldSaveWithActiveStatusAndReturnDto() {
        CustomerCreateRequest request =
                new CustomerCreateRequest("C001", "全家便利商店", "S001", "U001", "台北市", "02-1234");
        Customer saved = stubEntity("C001", ActiveStatus.ACTIVE);
        when(customerRepo.existsByCustomerCode("C001")).thenReturn(false);
        when(customerRepo.save(any(Customer.class))).thenReturn(saved);
        when(customerMapper.toDto(saved)).thenReturn(stubDto("C001", ActiveStatus.ACTIVE));

        CustomerDto result = customerService.create(request);

        assertThat(result.getCustomerCode()).isEqualTo("C001");
        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ActiveStatus.ACTIVE);
        assertThat(captor.getValue().getCustomerName()).isEqualTo("全家便利商店");
        assertThat(captor.getValue().getSalesOrgCode()).isEqualTo("S001");
    }

    @Test
    @DisplayName("create 代碼重複 - 應拋出 DuplicateResourceException 且不存檔")
    void create_whenCodeDuplicated_shouldThrowAndNotSave() {
        CustomerCreateRequest request =
                new CustomerCreateRequest("C001", "全家便利商店", "S001", "U001", null, null);
        when(customerRepo.existsByCustomerCode("C001")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("C001");
        verify(customerRepo, never()).save(any());
    }

    @Test
    @DisplayName("update - 應更新欄位並存檔，customerCode 不變")
    void update_shouldModifyFieldsAndSave() {
        Customer existing = stubEntity("C001", ActiveStatus.ACTIVE);
        CustomerUpdateRequest request =
                new CustomerUpdateRequest("萊爾富", "S002", "U002", "新北市", "02-9999");
        when(customerRepo.findByCustomerCode("C001")).thenReturn(Optional.of(existing));
        when(customerRepo.save(existing)).thenReturn(existing);
        when(customerMapper.toDto(existing)).thenReturn(stubDto("C001", ActiveStatus.ACTIVE));

        customerService.update("C001", request);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepo).save(captor.capture());
        Customer saved = captor.getValue();
        assertThat(saved.getCustomerCode()).isEqualTo("C001");       // 身份不變
        assertThat(saved.getCustomerName()).isEqualTo("萊爾富");       // 欄位已更新
        assertThat(saved.getSalesOrgCode()).isEqualTo("S002");
        assertThat(saved.getUserCode()).isEqualTo("U002");
    }

    @Test
    @DisplayName("update 不存在 - 應拋出 ResourceNotFoundException 且不存檔")
    void update_whenNotExists_shouldThrowResourceNotFound() {
        CustomerUpdateRequest request =
                new CustomerUpdateRequest("萊爾富", "S002", "U002", null, null);
        when(customerRepo.findByCustomerCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.update("UNKNOWN", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
        verify(customerRepo, never()).save(any());
    }

    @Test
    @DisplayName("delete - 應刪除存在的客戶")
    void delete_shouldRemoveCustomer() {
        Customer existing = stubEntity("C001", ActiveStatus.ACTIVE);
        when(customerRepo.findByCustomerCode("C001")).thenReturn(Optional.of(existing));

        customerService.delete("C001");

        verify(customerRepo).delete(existing);
    }

    @Test
    @DisplayName("delete 不存在 - 應拋出 ResourceNotFoundException 且不刪除")
    void delete_whenNotExists_shouldThrowResourceNotFound() {
        when(customerRepo.findByCustomerCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.delete("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
        verify(customerRepo, never()).delete(any());
    }
}
