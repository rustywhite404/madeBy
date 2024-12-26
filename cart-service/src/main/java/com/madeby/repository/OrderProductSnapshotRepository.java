package com.madeby.repository;

import com.madeby.entity.OrderProductSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderProductSnapshotRepository extends JpaRepository<OrderProductSnapshot, Long> {
}
