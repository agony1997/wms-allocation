package com.agony.wmsallocation.controller;

import com.agony.wmsallocation.dto.master.ProductCreateRequest;
import com.agony.wmsallocation.dto.master.ProductDto;
import com.agony.wmsallocation.dto.master.ProductUpdateRequest;
import com.agony.wmsallocation.security.RequireRole;
import com.agony.wmsallocation.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @RequireRole({"SALES", "LEADER", "WAREHOUSE", "ADMIN"})
    public List<ProductDto> findAll(@RequestParam(required = false) Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return productService.findAllActive();
        }
        return productService.findAll();
    }

    @GetMapping("/{productCode}")
    @RequireRole({"SALES", "LEADER", "WAREHOUSE", "ADMIN"})
    public ProductDto findByProductCode(@PathVariable String productCode) {
        return productService.findByProductCode(productCode);
    }

    @PostMapping
    @RequireRole("ADMIN")
    public ResponseEntity<ProductDto> create(@Valid @RequestBody ProductCreateRequest request) {
        ProductDto created = productService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{productCode}")
                .buildAndExpand(created.getProductCode())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{productCode}")
    @RequireRole("ADMIN")
    public ProductDto update(@PathVariable String productCode,
                             @Valid @RequestBody ProductUpdateRequest request) {
        return productService.update(productCode, request);
    }

    @DeleteMapping("/{productCode}")
    @RequireRole("ADMIN")
    public ResponseEntity<Void> delete(@PathVariable String productCode) {
        productService.delete(productCode);
        return ResponseEntity.noContent().build();
    }

}
