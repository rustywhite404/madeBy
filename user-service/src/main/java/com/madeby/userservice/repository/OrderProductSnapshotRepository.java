package com.madeby.userservice.repository;

import com.madeby.userservice.entity.OrderProductSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderProductSnapshotRepository extends JpaRepository<OrderProductSnapshot, Long> {
}
