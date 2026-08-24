package com.agony.wmsallocation.controller;

import com.agony.wmsallocation.dto.branch.BranchCreateRequest;
import com.agony.wmsallocation.dto.branch.BranchDto;
import com.agony.wmsallocation.dto.branch.BranchUpdateRequest;
import com.agony.wmsallocation.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    public List<BranchDto> findAll(@RequestParam(required = false) Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return branchService.findAllActive();
        }
        return branchService.findAll();
    }

    @GetMapping("/{branchCode}")
    public BranchDto findByBranchCode(@PathVariable String branchCode) {
        return branchService.findByBranchCode(branchCode);
    }

    @PostMapping
    public ResponseEntity<BranchDto> create(@Valid @RequestBody BranchCreateRequest request) {
        BranchDto created = branchService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{branchCode}")
                .buildAndExpand(created.getBranchCode())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{branchCode}")
    public BranchDto update(@PathVariable String branchCode,
                            @Valid @RequestBody BranchUpdateRequest request) {
        return branchService.update(branchCode, request);
    }

    @DeleteMapping("/{branchCode}")
    public ResponseEntity<Void> delete(@PathVariable String branchCode) {
        branchService.delete(branchCode);
        return ResponseEntity.noContent().build();
    }

}
