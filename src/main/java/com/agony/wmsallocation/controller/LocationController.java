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

    @GetMapping("/{branchCode}/{locationCode}")
    public LocationDto findByBranchCodeAndLocationCode(@PathVariable String branchCode,
                                                       @PathVariable String locationCode) {
        return locationService.findByBranchCodeAndLocationCode(branchCode, locationCode);
    }

}
