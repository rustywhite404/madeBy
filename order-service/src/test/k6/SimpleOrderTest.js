import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = 'http://localhost:9000/api/orders';
const AUTH_TOKEN = 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyOCIsImVtYWlsSGFzaCI6InNHVHcyQ1NSYWJRb28rSm5FUkQxZDl4M1haeDdqMzFNSEpFRTQ2ajJyV1U9IiwiYXV0aCI6IlVTRVIiLCJlbmFibGVkIjp0cnVlLCJleHAiOjE3MzYzNDA3ODQsImlhdCI6MTczNjMzNzE4NH0.iyYEaNEEloe889a3L226tCyVWYXNje5TYacbI7aEcJc';
const PRODUCT_ID = 32;
const INITIAL_STOCK = 10;
const TOTAL_USERS = 500;
const TEST_DURATION = '60s';

// 단순화된 메트릭스
const orderResults = new Trend('order_results');
const successOrders = new Counter('success_orders');
const failedOrders = new Counter('failed_orders');

export const options = {
    scenarios: {
        fixed_request_per_interval: {
            executor: 'per-vu-iterations',
            vus: TOTAL_USERS,
            iterations: 2,
            maxDuration: TEST_DURATION,
        }
    },
    thresholds: {
        'success_orders': [`count === ${INITIAL_STOCK}`], // 성공 주문이 초기 재고와 일치해야 함
        'order_results': ['avg < 3000'],
        'http_req_duration': ['p(95)<3000'],
    },
};

export default function () {
    const params = {
        headers: {
            'Authorization': AUTH_TOKEN,
            'Content-Type': 'application/json',
        },
    };

    const response = http.post(
        BASE_URL,
        JSON.stringify({
            productInfoId: PRODUCT_ID,
            quantity: 1,
        }),
        params
    );

    // 응답 시간 기록
    orderResults.add(response.timings.duration);

    // 상태 코드로 성공/실패 판단
    if (response.status === 200) {
        successOrders.add(1);
    } else if (response.status === 400) {
        failedOrders.add(1);
    }

    sleep(1);
}

export function handleSummary(data) {
    const successTotal = data.metrics.success_orders ? data.metrics.success_orders.values.count : 0;
    const failedTotal = data.metrics.failed_orders ? data.metrics.failed_orders.values.count : 0;

    const summary = `
== 주문 처리 결과 ==
총 요청 수: ${successTotal + failedTotal}
성공한 주문 (200): ${successTotal}
실패한 주문 (400): ${failedTotal}

테스트 결과: ${successTotal === INITIAL_STOCK ? '성공 ✅' : '실패 ❌'}
* 초기 재고(${INITIAL_STOCK})만큼 성공 주문이 있어야 합니다.
`;

    console.log(summary);
    return {};
}
