package com.madeby.productservice.elasticsearch;

import com.madeby.productservice.entity.ProductDocument;
import com.madeby.productservice.entity.Products;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ProductElasticsearchRepository extends ElasticsearchRepository<ProductDocument, Long> {
    @Query("{\"match\": {\"name\": {\"query\": \"?0\", \"analyzer\": \"nori\"}}}")
    Page<ProductDocument> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // 카테고리별 검색
    List<Products> findByCategory(String category);
}
