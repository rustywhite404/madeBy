package com.madeby.scheduler;

import com.madeby.entity.OrderProductSnapshot;
import com.madeby.entity.OrderStatus;
import com.madeby.entity.Orders;
import com.madeby.entity.ProductInfo;
import com.madeby.repository.OrderRepository;
import com.madeby.repository.ProductInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusScheduler {

    private final OrderRepository orderRepository;
    private final ProductInfoRepository productInfoRepository;

    @Scheduled(cron = "0 0 0,12,18 * * *") // 매일 00:00, 08:00, 16:00에 실행
    public void updateOrderStatus() {
        log.info("-------[제품 상태 업데이트 시작]-------");

        // 1. ORDERED 상태에서 SHIPPING 상태로 변경
        updateToShipping();

        // 2. SHIPPING 상태에서 DELIVERED 상태로 변경
        updateToDelivered();

        // 3. DELIVERED 상태에서 반품 가능 여부 변경
        updateReturnable();

        // 4. RETURN_REQUEST 상태에서 재고 업데이트 및 상태 변경
        processReturns();

        log.info("-------[제품 상태 업데이트 완료]-------");
    }

    // 1. ORDERED -> SHIPPING
    private void updateToShipping() {
        Long lastCursor = 0L; // 초기 커서
        int batchSize = 100;

        List<Orders> orders;

        do {
            orders = orderRepository.findByStatusAndCreatedAtBeforeWithCursor(
                    OrderStatus.ORDERED,
                    LocalDate.now().minusDays(1),
                    lastCursor,
                    batchSize
            );

            for (Orders order : orders) {
                order.setStatus(OrderStatus.SHIPPING);
                order.setDeliveryStartDate(LocalDate.now());
            }

            orderRepository.saveAll(orders);

            if (!orders.isEmpty()) {
                lastCursor = orders.get(orders.size() - 1).getId(); // 마지막 커서 업데이트
            }

        } while (!orders.isEmpty());

        log.info("[ORDERED -> SHIPPING 상태로 변경 완료]");
    }

    // 2. SHIPPING -> DELIVERED
    private void updateToDelivered() {
        Long lastCursor = 0L;
        int batchSize = 100;

        List<Orders> orders;

        do {
            orders = orderRepository.findByStatusAndCreatedAtBeforeWithCursor(
                    OrderStatus.SHIPPING,
                    LocalDate.now().minusDays(1),
                    lastCursor,
                    batchSize
            );

            for (Orders order : orders) {
                order.setStatus(OrderStatus.DELIVERED);
                order.setDeliveryEndDate(LocalDate.now());
            }

            orderRepository.saveAll(orders);

            if (!orders.isEmpty()) {
                lastCursor = orders.get(orders.size() - 1).getId();
            }

        } while (!orders.isEmpty());

        log.info("[SHIPPING -> DELIVERED 상태로 변경 완료]");
    }

    // 3. DELIVERED -> 반품 가능 여부 변경
    private void updateReturnable() {
        Long lastCursor = 0L;
        int batchSize = 100;

        List<Orders> orders;

        do {
            orders = orderRepository.findByStatusAndDeliveryEndDateBeforeWithCursor(
                    OrderStatus.DELIVERED,
                    LocalDate.now().minusDays(1),
                    lastCursor,
                    batchSize
            );

            for (Orders order : orders) {
                order.setReturnable(false);
            }

            orderRepository.saveAll(orders);

            if (!orders.isEmpty()) {
                lastCursor = orders.get(orders.size() - 1).getId();
            }

        } while (!orders.isEmpty());

        log.info("[DELIVERED 상품 : 반품 가능 여부 변경 완료]");
    }

    // 4. RETURN_REQUEST -> RETURNED
    // 재고 업데이트는 다른 상태 변경의 성공 여부와 관계 없이, 독립 수행되도록 트랜잭션 처리
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processReturns() {
        Long lastCursor = 0L;
        int batchSize = 100;

        List<Orders> orders;

        do {
            orders = orderRepository.findByStatusAndReturnRequestedDateBeforeWithCursor(
                    OrderStatus.RETURN_REQUEST,
                    LocalDate.now().minusDays(1),
                    lastCursor,
                    batchSize
            );

            for (Orders order : orders) {
                for (OrderProductSnapshot snapshot : order.getOrderProductSnapshots()) {
                    ProductInfo productInfo = productInfoRepository.findById(snapshot.getProductId())
                            .orElseThrow(() -> new IllegalArgumentException("Product not found"));

                    productInfo.setStock(productInfo.getStock() + snapshot.getQuantity());
                    productInfoRepository.save(productInfo);
                }
                order.setStatus(OrderStatus.RETURNED);
            }

            orderRepository.saveAll(orders);

            if (!orders.isEmpty()) {
                lastCursor = orders.get(orders.size() - 1).getId();
            }

        } while (!orders.isEmpty());

        log.info("[RETURN_REQUEST -> RETURNED 상태 변경 및 재고 업데이트 완료]");
    }
}