package com.agony.wmsallocation.controller;

import com.agony.wmsallocation.dto.branch.LocationDto;
import com.agony.wmsallocation.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    public List<LocationDto> findAll(@RequestParam(required = false) String branchCode,
                                     @RequestParam(required = false) Boolean activeOnly) {
        if (branchCode != null) {
            if (Boolean.TRUE.equals(activeOnly)) {
                return locationService.findByBranchCodeAndActive(branchCode);
            }
            return locationService.findByBranchCode(branchCode);
        }
        if (Boolean.TRUE.equals(activeOnly)) {
            return locationService.findAllActive();
        }
        return locationService.findAll();
    }

    // locationCode 全域唯一，路徑不需要再帶 branchCode
    @GetMapping("/{locationCode}")
    public LocationDto findByLocationCode(@PathVariable String locationCode) {
        return locationService.findByLocationCode(locationCode);
    }

}
