package com.madeby.orderservice.scheduler;

import com.madeBy.shared.exception.MadeByErrorCode;
import com.madeBy.shared.exception.MadeByException;
import com.madeby.orderservice.client.ProductServiceClient;
import com.madeby.orderservice.dto.ProductInfoDto;
import com.madeby.orderservice.entity.OrderProductSnapshot;
import com.madeby.orderservice.entity.OrderStatus;
import com.madeby.orderservice.entity.Orders;
import com.madeby.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusScheduler {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;

    @Scheduled(cron = "0 0 0,12,18 * * *") // 매일 00:00, 08:00, 16:00에 실행
    @Transactional
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
                    LocalDateTime.now().minusDays(1), // LocalDateTime으로 변환
                    lastCursor,
                    batchSize
            );

            for (Orders order : orders) {
                order.setStatus(OrderStatus.SHIPPING);
                order.setDeliveryStartDate(LocalDateTime.now());
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

            // 주문일(createdAt)로부터 2일이 지난 데이터 조회
            orders = orderRepository.findByStatusAndCreatedAtBeforeWithCursor(
                    OrderStatus.SHIPPING,
                    LocalDateTime.now().minusDays(2), // 2일 전 시간
                    lastCursor,
                    batchSize
            );

            for (Orders order : orders) {
                log.info("주문 ID: {}, 상태 변경: SHIPPING -> DELIVERED", order.getId());
                order.setStatus(OrderStatus.DELIVERED);
                order.setDeliveryEndDate(LocalDateTime.now()); // 배송 완료 시간 기록
            }

            // 변경된 상태를 저장
            orderRepository.saveAll(orders);

            if (!orders.isEmpty()) {
                lastCursor = orders.get(orders.size() - 1).getId(); // 마지막 처리된 주문 ID로 커서 업데이트
                log.info("lastCursor 업데이트: {}", lastCursor);
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
            // 배송 완료일(delivery_end_date)이 1일이 지난 데이터 조회
            orders = orderRepository.findByStatusAndDeliveryEndDateBeforeWithCursor(
                    OrderStatus.DELIVERED,
                    LocalDateTime.now().minusDays(1), // 1일 전 기준
                    lastCursor,
                    batchSize
            );

            for (Orders order : orders) {
                log.info("주문 ID: {}, 기존 반품 가능 여부: {}", order.getId(), order.isReturnable());
                order.setReturnable(false); // 반품 불가로 변경
                log.info("주문 ID: {}, 새로운 반품 가능 여부: {}", order.getId(), order.isReturnable());
            }

            // 변경된 상태를 저장
            orderRepository.saveAll(orders);

            if (!orders.isEmpty()) {
                lastCursor = orders.get(orders.size() - 1).getId(); // 마지막 커서 업데이트
                log.info("lastCursor 업데이트: {}", lastCursor);
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
                    LocalDateTime.now().minusDays(1),
                    lastCursor,
                    batchSize
            );

            for (Orders order : orders) {
                for (OrderProductSnapshot snapshot : order.getOrderProductSnapshots()) {
                    ProductInfoDto productInfo = productServiceClient.getProductInfo(snapshot.getProductInfoId());

                    if (productInfo == null) {
                        throw new MadeByException(MadeByErrorCode.NO_PRODUCT); // 예외 발생
                    }

                    // Feign Client를 통해 재고 업데이트 요청
                    boolean updateSuccess = productServiceClient.updateStock(
                            snapshot.getProductInfoId(),
                            snapshot.getQuantity()
                    );

                    if (!updateSuccess) {
                        throw new MadeByException(MadeByErrorCode.STOCK_UPDATE_FAILED); // 재고 업데이트 실패 시 예외 처리
                    }
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