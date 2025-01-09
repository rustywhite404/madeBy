import http from 'k6/http';
import {check, sleep} from 'k6';
import {Counter, Trend} from 'k6/metrics';

const BASE_URL = 'http://localhost:9000/api/orders';
const AUTH_TOKEN = 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyOCIsImVtYWlsSGFzaCI6InNHVHcyQ1NSYWJRb28rSm5FUkQxZDl4M1haeDdqMzFNSEpFRTQ2ajJyV1U9IiwiYXV0aCI6IlVTRVIiLCJlbmFibGVkIjp0cnVlLCJleHAiOjE3MzYzMzgwOTYsImlhdCI6MTczNjMzNDQ5Nn0.1ttjFQIHLDp7FZ1gPGcO4ca7B27GV0308nU7gx3-6xk';

const PRODUCT_ID = 32;
const INITIAL_STOCK = 10; // 초기 재고
const TOTAL_USERS = 2000; // 테스트에 참여할 사용자 수
const TEST_DURATION = '60s'; // 테스트 시간

// 메트릭스 정의 수정
const orderResults = new Trend('order_results');
const success = new Counter('success_total');
const failed = new Counter('failed_total');
const canceled = new Counter('canceled_total');
const soldOut = new Counter('soldout_total');

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
        'order_results': ['avg < 3000'],
        'success_total': ['count > 0'],
        'http_req_duration': ['p(95)<3000'],
    },
    noConnectionReuse: false,
    userAgent: 'k6-test',
    discardResponseBodies: true,
    maxRedirects: 0,
    batch: 10,
    batchPerHost: 10,
};

export default function () {
    const params = {
        headers: {
            'Authorization': AUTH_TOKEN,
            'Content-Type': 'application/json',
        },
        timeout: '5s',
        tags: { type: 'order' },
    };

    try {
        const startTime = new Date().getTime();
        const response = http.post(
            BASE_URL,
            JSON.stringify({
                productInfoId: PRODUCT_ID,
                quantity: 1,
            }),
            params
        );
        const duration = new Date().getTime() - startTime;

        // 응답 상태 체크 추가
        check(response, {
            'is status 200': (r) => r.status === 200,
        });

        orderResults.add(duration);

        if (response.status >= 400 || !response.body) {
            failed.add(1);
            return;
        }

        try {
            const data = JSON.parse(response.body);

            if (!data) {
                console.log('Empty response data');
                failed.add(1);
                return;
            }

            // 결과 타입별 카운터 증가 및 로깅
            if (data.success === false) {
                if (data.error.message.includes('CANCELED')) {
                    canceled.add(1);
                    console.log('Order canceled');
                } else if (data.error.message.includes('FAILED')) {
                    failed.add(1);
                    console.log('Order failed');
                } else if (data.error.code === 'NOT_ENOUGH_PRODUCT') {
                    soldOut.add(1);
                    console.log('Product sold out');
                }
            } else if (data.success === true) {
                success.add(1);
                console.log('Order success');
            } else {
                console.log('Unknown response format:', response.body);
                failed.add(1);
            }
        } catch (parseError) {
            console.log('JSON parse error:', parseError.message);
            failed.add(1);
        }
    } catch (requestError) {
        console.log('Request error:', requestError.message);
        failed.add(1);
    }

    sleep(30);
}

export function handleSummary(data) {
    const successTotal = data.metrics.success_total ? data.metrics.success_total.values.count : 0;
    const failedTotal = data.metrics.failed_total ? data.metrics.failed_total.values.count : 0;
    const canceledTotal = data.metrics.canceled_total ? data.metrics.canceled_total.values.count : 0;
    const soldOutTotal = data.metrics.soldout_total ? data.metrics.soldout_total.values.count : 0;

    const totalRequests = successTotal + failedTotal + canceledTotal + soldOutTotal;

    const totalFailedOrders = totalRequests - successTotal;
    const calculatedFailedOrders = failedTotal + canceledTotal + soldOutTotal;

    const validationResults = {
        '성공한 주문은 초기 재고와 같아야 한다': successTotal === INITIAL_STOCK,
        '실패한 주문 수는 실패 유형의 합과 같아야 한다': totalFailedOrders === calculatedFailedOrders,
        '실패한 주문은 결제 실패, 재고 부족, 이탈을 포함해야 한다': failedTotal === (totalRequests - successTotal - canceledTotal - soldOutTotal)
    };

    const summary = `
== 주문 처리 결과 ==
총 요청 수: ${totalRequests}
구매 성공: ${successTotal}
결제 실패: ${failedTotal}
결제 중 이탈: ${canceledTotal}
품절로 인한 구매 실패: ${soldOutTotal}

== 검증 결과 ==
성공한 주문은 초기 재고와 같아야 한다: ${validationResults['성공한 주문은 초기 재고와 같아야 한다']}
실패한 주문 수는 실패 유형의 합과 같아야 한다: ${validationResults['실패한 주문 수는 실패 유형의 합과 같아야 한다']}
실패한 주문은 결제 실패, 재고 부족, 이탈을 포함해야 한다: ${validationResults['실패한 주문은 결제 실패, 재고 부족, 이탈을 포함해야 한다']}

`;

    console.log(summary);
    return {};
}
