package com.madeby.productservice.repository;

import com.madeby.productservice.entity.Products;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductsRepository extends JpaRepository<Products, Long> {
    // isVisible이 true인 데이터만 조회 (최신 데이터부터 가져오기)
    Slice<Products> findAllByIsVisibleTrue(Pageable pageable);

    // isVisible이 true이고, id가 특정 값(cursor)보다 작은 데이터만 조회
    Slice<Products> findByIsVisibleTrueAndIdLessThan(Long id, Pageable pageable);

    // 상품과 연관된 ProductInfo를 함께 조회, 즉시 로딩을 강제로 설정(fetch join)
    @EntityGraph(attributePaths = {"productInfos"})
    Optional<Products> findByIdAndIsVisibleTrue(Long id);
}

