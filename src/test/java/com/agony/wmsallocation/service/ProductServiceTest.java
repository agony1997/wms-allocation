package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.master.ProductCreateRequest;
import com.agony.wmsallocation.dto.master.ProductDto;
import com.agony.wmsallocation.dto.master.ProductUpdateRequest;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import com.agony.wmsallocation.entity.master.Product;
import com.agony.wmsallocation.exception.DuplicateResourceException;
import com.agony.wmsallocation.exception.ResourceNotFoundException;
import com.agony.wmsallocation.mapper.ProductMapper;
import com.agony.wmsallocation.repository.ProductRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepo productRepo;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private Product stubEntity(String code, ActiveStatus status) {
        Product product = new Product();
        product.setProductCode(code);
        product.setProductName("測試商品");
        product.setBaseUnit("個");
        product.setBasePrice(new BigDecimal("100.00"));
        product.setStatus(status);
        return product;
    }

    private ProductDto stubDto(String code, ActiveStatus status) {
        return ProductDto.builder()
                .productCode(code)
                .productName("測試商品")
                .baseUnit("個")
                .basePrice(new BigDecimal("100.00"))
                .status(status)
                .build();
    }

    @Test
    @DisplayName("findAll - 應回傳所有商品")
    void findAll_shouldReturnAll() {
        Product entity = stubEntity("P001", ActiveStatus.ACTIVE);
        when(productRepo.findAll()).thenReturn(List.of(entity));
        when(productMapper.toDto(entity)).thenReturn(stubDto("P001", ActiveStatus.ACTIVE));

        List<ProductDto> result = productService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductCode()).isEqualTo("P001");
    }

    @Test
    @DisplayName("findAll 無資料 - 應回傳空清單")
    void findAll_whenEmpty_shouldReturnEmptyList() {
        when(productRepo.findAll()).thenReturn(List.of());

        assertThat(productService.findAll()).isEmpty();
    }

    @Test
    @DisplayName("findAllActive - 應以 ACTIVE 狀態查詢")
    void findAllActive_shouldQueryWithActiveStatus() {
        Product entity = stubEntity("P001", ActiveStatus.ACTIVE);
        when(productRepo.findByStatus(ActiveStatus.ACTIVE)).thenReturn(List.of(entity));
        when(productMapper.toDto(entity)).thenReturn(stubDto("P001", ActiveStatus.ACTIVE));

        List<ProductDto> result = productService.findAllActive();

        assertThat(result).hasSize(1);
        verify(productRepo).findByStatus(ActiveStatus.ACTIVE);
    }

    @Test
    @DisplayName("findByProductCode 存在 - 應回傳 DTO")
    void findByProductCode_whenExists_shouldReturnDto() {
        Product entity = stubEntity("P001", ActiveStatus.ACTIVE);
        when(productRepo.findByProductCode("P001")).thenReturn(Optional.of(entity));
        when(productMapper.toDto(entity)).thenReturn(stubDto("P001", ActiveStatus.ACTIVE));

        ProductDto result = productService.findByProductCode("P001");

        assertThat(result.getProductCode()).isEqualTo("P001");
    }

    @Test
    @DisplayName("findByProductCode 不存在 - 應拋出 ResourceNotFoundException")
    void findByProductCode_whenNotExists_shouldThrowResourceNotFound() {
        when(productRepo.findByProductCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findByProductCode("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    @DisplayName("create - 應預設 ACTIVE 並存檔回傳 DTO")
    void create_shouldSaveWithActiveStatusAndReturnDto() {
        ProductCreateRequest request =
                new ProductCreateRequest("P001", "可樂", "瓶", new BigDecimal("25.00"));
        Product saved = stubEntity("P001", ActiveStatus.ACTIVE);
        when(productRepo.existsByProductCode("P001")).thenReturn(false);
        when(productRepo.save(any(Product.class))).thenReturn(saved);
        when(productMapper.toDto(saved)).thenReturn(stubDto("P001", ActiveStatus.ACTIVE));

        ProductDto result = productService.create(request);

        assertThat(result.getProductCode()).isEqualTo("P001");
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepo).save(captor.capture());
        Product persisted = captor.getValue();
        assertThat(persisted.getStatus()).isEqualTo(ActiveStatus.ACTIVE);
        assertThat(persisted.getProductName()).isEqualTo("可樂");
        assertThat(persisted.getBaseUnit()).isEqualTo("瓶");
        assertThat(persisted.getBasePrice()).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("create 代碼重複 - 應拋出 DuplicateResourceException 且不存檔")
    void create_whenCodeDuplicated_shouldThrowAndNotSave() {
        ProductCreateRequest request =
                new ProductCreateRequest("P001", "可樂", "瓶", new BigDecimal("25.00"));
        when(productRepo.existsByProductCode("P001")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("P001");
        verify(productRepo, never()).save(any());
    }

    @Test
    @DisplayName("update - 應更新欄位並存檔，productCode 不變")
    void update_shouldModifyFieldsAndSave() {
        Product existing = stubEntity("P001", ActiveStatus.ACTIVE);
        ProductUpdateRequest request =
                new ProductUpdateRequest("雪碧", "罐", new BigDecimal("30.00"));
        when(productRepo.findByProductCode("P001")).thenReturn(Optional.of(existing));
        when(productRepo.save(existing)).thenReturn(existing);
        when(productMapper.toDto(existing)).thenReturn(stubDto("P001", ActiveStatus.ACTIVE));

        productService.update("P001", request);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepo).save(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved.getProductCode()).isEqualTo("P001");        // 身份不變
        assertThat(saved.getProductName()).isEqualTo("雪碧");         // 欄位已更新
        assertThat(saved.getBaseUnit()).isEqualTo("罐");
        assertThat(saved.getBasePrice()).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("update 不存在 - 應拋出 ResourceNotFoundException 且不存檔")
    void update_whenNotExists_shouldThrowResourceNotFound() {
        ProductUpdateRequest request =
                new ProductUpdateRequest("雪碧", "罐", new BigDecimal("30.00"));
        when(productRepo.findByProductCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update("UNKNOWN", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
        verify(productRepo, never()).save(any());
    }

    @Test
    @DisplayName("delete - 應刪除存在的商品")
    void delete_shouldRemoveProduct() {
        Product existing = stubEntity("P001", ActiveStatus.ACTIVE);
        when(productRepo.findByProductCode("P001")).thenReturn(Optional.of(existing));

        productService.delete("P001");

        verify(productRepo).delete(existing);
    }

    @Test
    @DisplayName("delete 不存在 - 應拋出 ResourceNotFoundException 且不刪除")
    void delete_whenNotExists_shouldThrowResourceNotFound() {
        when(productRepo.findByProductCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
        verify(productRepo, never()).delete(any());
    }
}
