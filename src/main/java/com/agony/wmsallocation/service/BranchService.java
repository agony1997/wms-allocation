package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.branch.BranchCreateRequest;
import com.agony.wmsallocation.dto.branch.BranchDto;
import com.agony.wmsallocation.dto.branch.BranchUpdateRequest;
import com.agony.wmsallocation.entity.branch.Branch;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import com.agony.wmsallocation.exception.DuplicateResourceException;
import com.agony.wmsallocation.exception.ErrorCode;
import com.agony.wmsallocation.exception.ResourceInUseException;
import com.agony.wmsallocation.exception.ResourceNotFoundException;
import com.agony.wmsallocation.mapper.BranchMapper;
import com.agony.wmsallocation.repository.AuthUserRepo;
import com.agony.wmsallocation.repository.BranchRepo;
import com.agony.wmsallocation.repository.LocationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class BranchService {

    private final BranchRepo branchRepo;
    private final BranchMapper branchMapper;
    private final LocationRepo locationRepo;
    private final AuthUserRepo authUserRepo;

    public List<BranchDto> findAll() {
        return branchRepo.findAll().stream()
                .map(branchMapper::toDto)
                .toList();
    }

    public List<BranchDto> findAllActive() {
        return branchRepo.findByStatus(ActiveStatus.ACTIVE).stream()
                .map(branchMapper::toDto)
                .toList();
    }

    public BranchDto findByBranchCode(String branchCode) {
        return branchRepo.findByBranchCode(branchCode)
                .map(branchMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("找不到營業所：branchCode=" + branchCode));
    }

    public BranchDto create(BranchCreateRequest request) {
        if (branchRepo.existsByBranchCode(request.branchCode())) {
            throw new DuplicateResourceException(
                    "營業所代碼已存在：branchCode=" + request.branchCode(),
                    ErrorCode.BRANCH_CODE_DUPLICATED);
        }

        Branch branch = new Branch();
        branch.setBranchCode(request.branchCode());
        branch.setSalesOrgCode(request.salesOrgCode());
        branch.setBranchName(request.branchName());
        branch.setAddress(request.address());
        branch.setPhone(request.phone());
        branch.setStatus(ActiveStatus.ACTIVE);   // 建立一律啟用，前端不指定

        return branchMapper.toDto(branchRepo.save(branch));
    }

    public BranchDto update(String branchCode, BranchUpdateRequest request) {
        Branch branch = branchRepo.findByBranchCode(branchCode)
                .orElseThrow(() -> new ResourceNotFoundException("找不到營業所：branchCode=" + branchCode));

        branch.setSalesOrgCode(request.salesOrgCode());
        branch.setBranchName(request.branchName());
        branch.setAddress(request.address());
        branch.setPhone(request.phone());
        // branchCode 不可改（身份）；status 由獨立的啟用/停用操作處理

        return branchMapper.toDto(branchRepo.save(branch));
    }

    public void delete(String branchCode) {
        Branch branch = branchRepo.findByBranchCode(branchCode)
                .orElseThrow(() -> new ResourceNotFoundException("找不到營業所：branchCode=" + branchCode));

        List<String> dependents = new ArrayList<>();
        if (locationRepo.existsByBranchCode(branchCode)) dependents.add("儲位");
        if (authUserRepo.existsByBranchCode(branchCode)) dependents.add("人員");
        // ponytail: 目前僅檢查主檔下轄（儲位/人員）；庫存與各類單據等營運資料待該模組成熟後再納入
        if (!dependents.isEmpty()) {
            throw new ResourceInUseException(
                    "營業所尚有下轄資料，無法刪除：branchCode=" + branchCode + "（" + String.join("、", dependents) + "）",
                    ErrorCode.BRANCH_HAS_DEPENDENTS);
        }

        branchRepo.delete(branch);
    }

}
