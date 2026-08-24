package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.master.FactoryCreateRequest;
import com.agony.wmsallocation.dto.master.FactoryDto;
import com.agony.wmsallocation.dto.master.FactoryUpdateRequest;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import com.agony.wmsallocation.entity.master.Factory;
import com.agony.wmsallocation.exception.DuplicateResourceException;
import com.agony.wmsallocation.exception.ErrorCode;
import com.agony.wmsallocation.exception.ResourceNotFoundException;
import com.agony.wmsallocation.mapper.FactoryMapper;
import com.agony.wmsallocation.repository.FactoryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class FactoryService {

    private final FactoryRepo factoryRepo;
    private final FactoryMapper factoryMapper;

    public List<FactoryDto> findAll() {
        return factoryRepo.findAll().stream()
                .map(factoryMapper::toDto)
                .toList();
    }

    public List<FactoryDto> findAllActive() {
        return factoryRepo.findByStatus(ActiveStatus.ACTIVE).stream()
                .map(factoryMapper::toDto)
                .toList();
    }

    public FactoryDto findByFactoryCode(String factoryCode) {
        return factoryRepo.findByFactoryCode(factoryCode)
                .map(factoryMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("找不到工廠：factoryCode=" + factoryCode));
    }

    public FactoryDto create(FactoryCreateRequest request) {
        if (factoryRepo.existsByFactoryCode(request.factoryCode())) {
            throw new DuplicateResourceException(
                    "工廠代碼已存在：factoryCode=" + request.factoryCode(),
                    ErrorCode.FACTORY_CODE_DUPLICATED);
        }

        Factory factory = new Factory();
        factory.setFactoryCode(request.factoryCode());
        factory.setFactoryName(request.factoryName());
        factory.setAddress(request.address());
        factory.setPhone(request.phone());
        factory.setStatus(ActiveStatus.ACTIVE);   // 建立一律啟用，前端不指定

        return factoryMapper.toDto(factoryRepo.save(factory));
    }

    public FactoryDto update(String factoryCode, FactoryUpdateRequest request) {
        Factory factory = factoryRepo.findByFactoryCode(factoryCode)
                .orElseThrow(() -> new ResourceNotFoundException("找不到工廠：factoryCode=" + factoryCode));

        factory.setFactoryName(request.factoryName());
        factory.setAddress(request.address());
        factory.setPhone(request.phone());
        // factoryCode 不可改（身份）；status 由獨立的啟用/停用操作處理

        return factoryMapper.toDto(factoryRepo.save(factory));
    }

    public void delete(String factoryCode) {
        Factory factory = factoryRepo.findByFactoryCode(factoryCode)
                .orElseThrow(() -> new ResourceNotFoundException("找不到工廠：factoryCode=" + factoryCode));

        // ponytail: ProductFactory 尚無 Repo，下轄檢查（產品對應）待該表建置後補上
        factoryRepo.delete(factory);
    }

}
