package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.master.ProductCreateRequest;
import com.agony.wmsallocation.dto.master.ProductDto;
import com.agony.wmsallocation.dto.master.ProductUpdateRequest;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import com.agony.wmsallocation.entity.master.Product;
import com.agony.wmsallocation.exception.DuplicateResourceException;
import com.agony.wmsallocation.exception.ErrorCode;
import com.agony.wmsallocation.exception.ResourceNotFoundException;
import com.agony.wmsallocation.mapper.ProductMapper;
import com.agony.wmsallocation.repository.ProductRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductRepo productRepo;
    private final ProductMapper productMapper;

    public List<ProductDto> findAll() {
        return productRepo.findAll().stream()
                .map(productMapper::toDto)
                .toList();
    }

    public List<ProductDto> findAllActive() {
        return productRepo.findByStatus(ActiveStatus.ACTIVE).stream()
                .map(productMapper::toDto)
                .toList();
    }

    public ProductDto findByProductCode(String productCode) {
        return productRepo.findByProductCode(productCode)
                .map(productMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("找不到商品：productCode=" + productCode));
    }

    public ProductDto create(ProductCreateRequest request) {
        if (productRepo.existsByProductCode(request.productCode())) {
            throw new DuplicateResourceException(
                    "商品代碼已存在：productCode=" + request.productCode(),
                    ErrorCode.PRODUCT_CODE_DUPLICATED);
        }

        Product product = new Product();
        product.setProductCode(request.productCode());
        product.setProductName(request.productName());
        product.setBaseUnit(request.baseUnit());
        product.setBasePrice(request.basePrice());
        product.setStatus(ActiveStatus.ACTIVE);   // 建立一律啟用，前端不指定

        return productMapper.toDto(productRepo.save(product));
    }

    public ProductDto update(String productCode, ProductUpdateRequest request) {
        Product product = productRepo.findByProductCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("找不到商品：productCode=" + productCode));

        product.setProductName(request.productName());
        product.setBaseUnit(request.baseUnit());
        product.setBasePrice(request.basePrice());
        // productCode 不可改（身份）；status 由獨立的啟用/停用操作處理

        return productMapper.toDto(productRepo.save(product));
    }

    public void delete(String productCode) {
        Product product = productRepo.findByProductCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("找不到商品：productCode=" + productCode));

        // ponytail: ProductFactory/ProductUnitConversion/BranchProductList 尚無 Repo，下轄檢查待該表建置後補上
        productRepo.delete(product);
    }

}
