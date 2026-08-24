package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.master.SalesOrganizationCreateRequest;
import com.agony.wmsallocation.dto.master.SalesOrganizationDto;
import com.agony.wmsallocation.dto.master.SalesOrganizationUpdateRequest;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import com.agony.wmsallocation.entity.master.SalesOrganization;
import com.agony.wmsallocation.exception.DuplicateResourceException;
import com.agony.wmsallocation.exception.ErrorCode;
import com.agony.wmsallocation.exception.ResourceInUseException;
import com.agony.wmsallocation.exception.ResourceNotFoundException;
import com.agony.wmsallocation.mapper.SalesOrganizationMapper;
import com.agony.wmsallocation.repository.BranchRepo;
import com.agony.wmsallocation.repository.CustomerRepo;
import com.agony.wmsallocation.repository.SalesOrganizationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class SalesOrganizationService {

    private final SalesOrganizationRepo salesOrganizationRepo;
    private final SalesOrganizationMapper salesOrganizationMapper;
    private final BranchRepo branchRepo;
    private final CustomerRepo customerRepo;

    public List<SalesOrganizationDto> findAll() {
        return salesOrganizationRepo.findAll().stream()
                .map(salesOrganizationMapper::toDto)
                .toList();
    }

    public List<SalesOrganizationDto> findAllActive() {
        return salesOrganizationRepo.findByStatus(ActiveStatus.ACTIVE).stream()
                .map(salesOrganizationMapper::toDto)
                .toList();
    }

    public SalesOrganizationDto findBySalesOrgCode(String salesOrgCode) {
        return salesOrganizationRepo.findBySalesOrgCode(salesOrgCode)
                .map(salesOrganizationMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("找不到銷售組織：salesOrgCode=" + salesOrgCode));
    }

    public SalesOrganizationDto create(SalesOrganizationCreateRequest request) {
        if (salesOrganizationRepo.existsBySalesOrgCode(request.salesOrgCode())) {
            throw new DuplicateResourceException(
                    "銷售組織代碼已存在：salesOrgCode=" + request.salesOrgCode(),
                    ErrorCode.SALES_ORG_CODE_DUPLICATED);
        }

        SalesOrganization salesOrg = new SalesOrganization();
        salesOrg.setSalesOrgCode(request.salesOrgCode());
        salesOrg.setSalesOrgName(request.salesOrgName());
        salesOrg.setStatus(ActiveStatus.ACTIVE);   // 建立一律啟用，前端不指定

        return salesOrganizationMapper.toDto(salesOrganizationRepo.save(salesOrg));
    }

    public SalesOrganizationDto update(String salesOrgCode, SalesOrganizationUpdateRequest request) {
        SalesOrganization salesOrg = salesOrganizationRepo.findBySalesOrgCode(salesOrgCode)
                .orElseThrow(() -> new ResourceNotFoundException("找不到銷售組織：salesOrgCode=" + salesOrgCode));

        salesOrg.setSalesOrgName(request.salesOrgName());
        // salesOrgCode 不可改（身份）；status 由獨立的啟用/停用操作處理

        return salesOrganizationMapper.toDto(salesOrganizationRepo.save(salesOrg));
    }

    public void delete(String salesOrgCode) {
        SalesOrganization salesOrg = salesOrganizationRepo.findBySalesOrgCode(salesOrgCode)
                .orElseThrow(() -> new ResourceNotFoundException("找不到銷售組織：salesOrgCode=" + salesOrgCode));

        List<String> dependents = new ArrayList<>();
        if (branchRepo.existsBySalesOrgCode(salesOrgCode)) dependents.add("營業所");
        if (customerRepo.existsBySalesOrgCode(salesOrgCode)) dependents.add("客戶");
        if (!dependents.isEmpty()) {
            throw new ResourceInUseException(
                    "銷售組織尚有下轄資料，無法刪除：salesOrgCode=" + salesOrgCode + "（" + String.join("、", dependents) + "）",
                    ErrorCode.SALES_ORG_HAS_DEPENDENTS);
        }

        salesOrganizationRepo.delete(salesOrg);
    }

}
