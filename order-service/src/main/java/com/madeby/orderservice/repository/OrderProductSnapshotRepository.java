package com.madeby.orderservice.repository;

import com.madeby.orderservice.entity.OrderProductSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderProductSnapshotRepository extends JpaRepository<OrderProductSnapshot, Long> {
}
