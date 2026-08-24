package com.agony.wmsallocation.repository;

import com.agony.wmsallocation.entity.master.ProductFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductFactoryRepo extends JpaRepository<ProductFactory, Integer> {

    List<ProductFactory> findByProductCodeInAndIsDefaultTrue(List<String> productCodes);
}
