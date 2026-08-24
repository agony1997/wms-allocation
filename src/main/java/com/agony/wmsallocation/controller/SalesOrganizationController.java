package com.agony.wmsallocation.controller;

import com.agony.wmsallocation.dto.master.SalesOrganizationCreateRequest;
import com.agony.wmsallocation.dto.master.SalesOrganizationDto;
import com.agony.wmsallocation.dto.master.SalesOrganizationUpdateRequest;
import com.agony.wmsallocation.service.SalesOrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/sales-orgs")
public class SalesOrganizationController {

    private final SalesOrganizationService salesOrganizationService;

    @GetMapping
    public List<SalesOrganizationDto> findAll(@RequestParam(required = false) Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return salesOrganizationService.findAllActive();
        }
        return salesOrganizationService.findAll();
    }

    @GetMapping("/{salesOrgCode}")
    public SalesOrganizationDto findBySalesOrgCode(@PathVariable String salesOrgCode) {
        return salesOrganizationService.findBySalesOrgCode(salesOrgCode);
    }

    @PostMapping
    public ResponseEntity<SalesOrganizationDto> create(@Valid @RequestBody SalesOrganizationCreateRequest request) {
        SalesOrganizationDto created = salesOrganizationService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{salesOrgCode}")
                .buildAndExpand(created.getSalesOrgCode())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{salesOrgCode}")
    public SalesOrganizationDto update(@PathVariable String salesOrgCode,
                                       @Valid @RequestBody SalesOrganizationUpdateRequest request) {
        return salesOrganizationService.update(salesOrgCode, request);
    }

    @DeleteMapping("/{salesOrgCode}")
    public ResponseEntity<Void> delete(@PathVariable String salesOrgCode) {
        salesOrganizationService.delete(salesOrgCode);
        return ResponseEntity.noContent().build();
    }

}
