package com.madeby.productservice.repository;

import com.madeby.productservice.entity.ProductInfo;
import com.madeby.productservice.entity.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductInfoRepository extends JpaRepository<ProductInfo, Long> {
    @Modifying
    @Query("UPDATE ProductInfo p SET p.stock = p.stock - :quantity WHERE p.id = :id AND p.stock >= :quantity")
    int decrementStock(@Param("id") Long id, @Param("quantity") int quantity);

    List<ProductInfo> findByIsLimitedTrueAndIsVisibleFalse();

    boolean existsByProductsAndColorAndSize(Products product, String color, String size);
}
