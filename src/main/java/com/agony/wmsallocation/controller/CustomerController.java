package com.agony.wmsallocation.controller;

import com.agony.wmsallocation.dto.master.CustomerCreateRequest;
import com.agony.wmsallocation.dto.master.CustomerDto;
import com.agony.wmsallocation.dto.master.CustomerUpdateRequest;
import com.agony.wmsallocation.security.RequireRole;
import com.agony.wmsallocation.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @RequireRole({"SALES", "LEADER", "WAREHOUSE", "ADMIN"})
    public List<CustomerDto> findAll(@RequestParam(required = false) Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return customerService.findAllActive();
        }
        return customerService.findAll();
    }

    @GetMapping("/{customerCode}")
    @RequireRole({"SALES", "LEADER", "WAREHOUSE", "ADMIN"})
    public CustomerDto findByCustomerCode(@PathVariable String customerCode) {
        return customerService.findByCustomerCode(customerCode);
    }

    @PostMapping
    @RequireRole("ADMIN")
    public ResponseEntity<CustomerDto> create(@Valid @RequestBody CustomerCreateRequest request) {
        CustomerDto created = customerService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{customerCode}")
                .buildAndExpand(created.getCustomerCode())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{customerCode}")
    @RequireRole("ADMIN")
    public CustomerDto update(@PathVariable String customerCode,
                              @Valid @RequestBody CustomerUpdateRequest request) {
        return customerService.update(customerCode, request);
    }

    @DeleteMapping("/{customerCode}")
    @RequireRole("ADMIN")
    public ResponseEntity<Void> delete(@PathVariable String customerCode) {
        customerService.delete(customerCode);
        return ResponseEntity.noContent().build();
    }

}
