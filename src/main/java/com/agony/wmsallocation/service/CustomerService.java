package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.master.CustomerCreateRequest;
import com.agony.wmsallocation.dto.master.CustomerDto;
import com.agony.wmsallocation.dto.master.CustomerUpdateRequest;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import com.agony.wmsallocation.entity.master.Customer;
import com.agony.wmsallocation.exception.DuplicateResourceException;
import com.agony.wmsallocation.exception.ErrorCode;
import com.agony.wmsallocation.exception.ResourceNotFoundException;
import com.agony.wmsallocation.mapper.CustomerMapper;
import com.agony.wmsallocation.repository.CustomerRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CustomerService {

    private final CustomerRepo customerRepo;
    private final CustomerMapper customerMapper;

    public List<CustomerDto> findAll() {
        return customerRepo.findAll().stream()
                .map(customerMapper::toDto)
                .toList();
    }

    public List<CustomerDto> findAllActive() {
        return customerRepo.findByStatus(ActiveStatus.ACTIVE).stream()
                .map(customerMapper::toDto)
                .toList();
    }

    public CustomerDto findByCustomerCode(String customerCode) {
        return customerRepo.findByCustomerCode(customerCode)
                .map(customerMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("找不到客戶：customerCode=" + customerCode));
    }

    public CustomerDto create(CustomerCreateRequest request) {
        if (customerRepo.existsByCustomerCode(request.customerCode())) {
            throw new DuplicateResourceException(
                    "客戶代碼已存在：customerCode=" + request.customerCode(),
                    ErrorCode.CUSTOMER_CODE_DUPLICATED);
        }

        Customer customer = new Customer();
        customer.setCustomerCode(request.customerCode());
        customer.setCustomerName(request.customerName());
        customer.setSalesOrgCode(request.salesOrgCode());
        customer.setUserCode(request.userCode());
        customer.setAddress(request.address());
        customer.setPhone(request.phone());
        customer.setStatus(ActiveStatus.ACTIVE);   // 建立一律啟用，前端不指定

        return customerMapper.toDto(customerRepo.save(customer));
    }

    public CustomerDto update(String customerCode, CustomerUpdateRequest request) {
        Customer customer = customerRepo.findByCustomerCode(customerCode)
                .orElseThrow(() -> new ResourceNotFoundException("找不到客戶：customerCode=" + customerCode));

        customer.setCustomerName(request.customerName());
        customer.setSalesOrgCode(request.salesOrgCode());
        customer.setUserCode(request.userCode());
        customer.setAddress(request.address());
        customer.setPhone(request.phone());
        // customerCode 不可改（身份）；status 由獨立的啟用/停用操作處理

        return customerMapper.toDto(customerRepo.save(customer));
    }

    public void delete(String customerCode) {
        Customer customer = customerRepo.findByCustomerCode(customerCode)
                .orElseThrow(() -> new ResourceNotFoundException("找不到客戶：customerCode=" + customerCode));

        // ponytail: 目前無其他主檔表引用 customerCode，故不做下轄檢查；CPO/SDO/AR 等營運資料待該模組成熟後再納入
        customerRepo.delete(customer);
    }

}
