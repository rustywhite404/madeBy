package com.madeby.repository;

import com.madeby.entity.Products;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductsRepository extends JpaRepository<Products, Long> {
    // isVisible이 true인 데이터만 조회 (최신 데이터부터 가져오기)
    Slice<Products> findAllByIsVisibleTrue(Pageable pageable);

    // isVisible이 true이고, id가 특정 값(cursor)보다 작은 데이터만 조회
    Slice<Products> findByIsVisibleTrueAndIdLessThan(Long id, Pageable pageable);
}

