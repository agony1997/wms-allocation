package com.agony.wmsallocation.controller;

import com.agony.wmsallocation.dto.master.FactoryCreateRequest;
import com.agony.wmsallocation.dto.master.FactoryDto;
import com.agony.wmsallocation.dto.master.FactoryUpdateRequest;
import com.agony.wmsallocation.security.RequireRole;
import com.agony.wmsallocation.service.FactoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/factories")
public class FactoryController {

    private final FactoryService factoryService;

    @GetMapping
    @RequireRole({"SALES", "LEADER", "WAREHOUSE", "ADMIN"})
    public List<FactoryDto> findAll(@RequestParam(required = false) Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return factoryService.findAllActive();
        }
        return factoryService.findAll();
    }

    @GetMapping("/{factoryCode}")
    @RequireRole({"SALES", "LEADER", "WAREHOUSE", "ADMIN"})
    public FactoryDto findByFactoryCode(@PathVariable String factoryCode) {
        return factoryService.findByFactoryCode(factoryCode);
    }

    @PostMapping
    @RequireRole("ADMIN")
    public ResponseEntity<FactoryDto> create(@Valid @RequestBody FactoryCreateRequest request) {
        FactoryDto created = factoryService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{factoryCode}")
                .buildAndExpand(created.getFactoryCode())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{factoryCode}")
    @RequireRole("ADMIN")
    public FactoryDto update(@PathVariable String factoryCode,
                             @Valid @RequestBody FactoryUpdateRequest request) {
        return factoryService.update(factoryCode, request);
    }

    @DeleteMapping("/{factoryCode}")
    @RequireRole("ADMIN")
    public ResponseEntity<Void> delete(@PathVariable String factoryCode) {
        factoryService.delete(factoryCode);
        return ResponseEntity.noContent().build();
    }

}
