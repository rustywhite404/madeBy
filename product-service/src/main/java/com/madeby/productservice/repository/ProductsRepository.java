package com.madeby.productservice.repository;

import com.madeby.productservice.dto.ProductsDto;
import com.madeby.productservice.dto.ProductsWithoutInfoDto;
import com.madeby.productservice.entity.Products;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductsRepository extends JpaRepository<Products, Long> {
    // isVisible이 true인 데이터만 조회 (최신 데이터부터 가져오기)
    Slice<Products> findAllByIsVisibleTrue(Pageable pageable);

    // isVisible이 true이고, id가 특정 값(cursor)보다 작은 데이터만 조회
    Slice<Products> findByIsVisibleTrueAndIdLessThan(Long id, Pageable pageable);

    // 상품과 연관된 ProductInfo를 함께 조회, 즉시 로딩을 강제로 설정(fetch join)
    @EntityGraph(attributePaths = {"productInfos"})
    Optional<Products> findByIdAndIsVisibleTrue(Long id);

    //name으로 검색
    List<Products> findByNameContainingAndIsVisibleTrue(String name);

    // N+1 문제 해결을 위한 fetch join 쿼리 추가
    @Query("SELECT DISTINCT p FROM Products p LEFT JOIN FETCH p.productInfos WHERE p.isVisible = true")
    List<Products> findAllWithProductInfos();

    @Query("SELECT DISTINCT p FROM Products p LEFT JOIN FETCH p.productInfos pi " +
            "WHERE p.isVisible = true AND p.id < :cursor " +
            "ORDER BY p.id DESC")
    Slice<Products> findByIdLessThanWithProductInfos(@Param("cursor") Long cursor, Pageable pageable);

    // 커서 기반 검색 쿼리 추가
    @Query("SELECT new com.madeby.productservice.dto.ProductsWithoutInfoDto(p.id, p.name, p.image, p.description, p.category) " +
            "FROM Products p " +
            "WHERE p.isVisible = true " +
            "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "AND (p.id < :cursor OR :cursor IS NULL) " +
            "ORDER BY p.id DESC")
    List<ProductsWithoutInfoDto> searchByNameWithCursor(@Param("name") String name,
                                                        @Param("cursor") Long cursor,
                                                        Pageable pageable);

    @Query("SELECT new com.madeby.productservice.dto.ProductsWithoutInfoDto(p.id, p.name, p.image, p.description, p.category) " +
            "FROM Products p " +
            "WHERE p.isVisible = true AND p.id < :cursor " +
            "ORDER BY p.id DESC")
    List<ProductsWithoutInfoDto> findByIdLessThanWithoutProductInfos(@Param("cursor") Long cursor, Pageable pageable);

}

