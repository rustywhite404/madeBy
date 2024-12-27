package com.madeby.productservice.repository;

import com.madeby.productservice.entity.OrderProductSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderProductSnapshotRepository extends JpaRepository<OrderProductSnapshot, Long> {
}
