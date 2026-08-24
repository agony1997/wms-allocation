package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.branch.LocationDto;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import com.agony.wmsallocation.exception.ResourceNotFoundException;
import com.agony.wmsallocation.mapper.LocationMapper;
import com.agony.wmsallocation.repository.LocationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class LocationService {

    private final LocationRepo locationRepo;
    private final LocationMapper locationMapper;

    public List<LocationDto> findAll() {
        return locationRepo.findAll().stream()
                .map(locationMapper::toDto)
                .toList();
    }

    public List<LocationDto> findAllActive() {
        return locationRepo.findByStatus(ActiveStatus.ACTIVE).stream()
                .map(locationMapper::toDto)
                .toList();
    }

    public List<LocationDto> findByBranchCode(String branchCode) {
        return locationRepo.findByBranchCode(branchCode).stream()
                .map(locationMapper::toDto)
                .toList();
    }

    public List<LocationDto> findByBranchCodeAndActive(String branchCode) {
        return locationRepo.findByBranchCodeAndStatus(branchCode, ActiveStatus.ACTIVE).stream()
                .map(locationMapper::toDto)
                .toList();
    }

    public LocationDto findByBranchCodeAndLocationCode(String branchCode, String locationCode) {
        return locationRepo.findByBranchCodeAndLocationCode(branchCode, locationCode)
                .map(locationMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到儲位：branchCode=" + branchCode + ", locationCode=" + locationCode));
    }

}
