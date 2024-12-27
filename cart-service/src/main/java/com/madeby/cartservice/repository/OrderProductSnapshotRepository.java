package com.madeby.cartservice.repository;

import com.madeby.cartservice.entity.OrderProductSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderProductSnapshotRepository extends JpaRepository<OrderProductSnapshot, Long> {
}
