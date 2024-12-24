package com.madeby.repository;

import com.madeby.entity.OrderStatus;
import com.madeby.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OrderRepository extends JpaRepository<Orders, Long> {
    @Query("SELECT o FROM Orders o " +
            "WHERE o.status = :status " +
            "AND o.createdAt < :beforeDate " +
            "AND o.id > :lastCursor " +
            "ORDER BY o.id ASC")
    List<Orders> findByStatusAndCreatedAtBeforeWithCursor(
            @Param("status") OrderStatus status,
            @Param("beforeDate") LocalDate beforeDate,
            @Param("lastCursor") Long lastCursor,
            @Param("batchSize") int batchSize
    );

    @Query("SELECT o FROM Orders o " +
            "WHERE o.status = :status " +
            "AND o.deliveryEndDate < :beforeDate " +
            "AND o.id > :lastCursor " +
            "ORDER BY o.id ASC")
    List<Orders> findByStatusAndDeliveryEndDateBeforeWithCursor(
            @Param("status") OrderStatus status,
            @Param("beforeDate") LocalDate beforeDate,
            @Param("lastCursor") Long lastCursor,
            @Param("batchSize") int batchSize
    );


    @Query("SELECT o FROM Orders o " +
            "WHERE o.status = :status " +
            "AND o.returnRequestedDate < :beforeDate " +
            "AND o.id > :lastCursor " +
            "ORDER BY o.id ASC")
    List<Orders> findByStatusAndReturnRequestedDateBeforeWithCursor(
            @Param("status") OrderStatus status,
            @Param("beforeDate") LocalDate beforeDate,
            @Param("lastCursor") Long lastCursor,
            @Param("batchSize") int batchSize
    );
    @Query("SELECT o FROM Orders o " +
            "WHERE o.user.id = :userId " +
            "AND o.createdAt BETWEEN :startDate AND :endDate " +
            "AND o.id > :cursor " +
            "ORDER BY o.id ASC")
    List<Orders> findOrdersByUserIdAndDateWithCursor(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("cursor") Long cursor,
            @Param("size") int size
    );

}
