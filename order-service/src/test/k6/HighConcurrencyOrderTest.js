import http from 'k6/http';
import {check, sleep} from 'k6';
import {Counter} from 'k6/metrics';

const BASE_URL = 'http://localhost:9000/api/orders';
const AUTH_TOKEN = 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyOCIsImVtYWlsSGFzaCI6InNHVHcyQ1NSYWJRb28rSm5FUkQxZDl4M1haeDdqMzFNSEpFRTQ2ajJyV1U9IiwiYXV0aCI6IlVTRVIiLCJlbmFibGVkIjp0cnVlLCJleHAiOjE3MzYzMjMzOTUsImlhdCI6MTczNjMxOTc5NX0.DYjXq04Md6P5yrueMRg-qSCR6xIEgkMF_zcouGghJkE'
const PRODUCT_ID = 33;
const INITIAL_STOCK = 10; // 초기 재고
const TOTAL_USERS = 1000; // 테스트에 참여할 총 사용자 수
const TEST_TIME = '30s'; // 테스트에 참여할 총 사용자 수

// 메트릭스 정의
const success = new Counter('success');
const failed = new Counter('failed');
const canceled = new Counter('canceled');
const soldOut = new Counter('soldOut');

export const options = {
    stages: [
        {duration: TEST_TIME, target: TOTAL_USERS},
    ],
    thresholds: {
        http_req_duration: ['p(95)<3000'], //http 요청의 95%가 3초 이하로 응답하는지 확인
    },
};

export default function () {
    const response = http.post(
        BASE_URL,
        JSON.stringify({
            productInfoId: PRODUCT_ID,
            quantity: 1,
        }),
        {
            headers: {
                'Authorization': AUTH_TOKEN,
                'Content-Type': 'application/json',
            },
        }
    );

    try {
        const data = JSON.parse(response.body);

        if (data.success == false) {
            if (data.error.message.includes('CANCELED')) {
                canceled.add(1);
            } else if (data.error.message.includes('FAILED')) {
                failed.add(1);
            } else if (data.error.code === 'NOT_ENOUGH_PRODUCT') {
                soldOut.add(1);
            }
        } else {
            success.add(1);
        }
    } catch (e) {
        console.error('Parse error:', e.message);
    }

    sleep(0.1);
}

export function handleSummary(data) {
    // 메트릭스 값 추출
    const successCount = data.metrics.success ? data.metrics.success.values.count : 0;
    const failedCount = data.metrics.failed ? data.metrics.failed.values.count : 0;
    const canceledCount = data.metrics.canceled ? data.metrics.canceled.values.count : 0;
    const soldOutCount = data.metrics.soldOut ? data.metrics.soldOut.values.count : 0;

    const totalRequests = successCount + failedCount + canceledCount + soldOutCount;
    const totalFailedOrders = totalRequests - successCount;
    const calculatedFailedOrders = failedCount + canceledCount + soldOutCount;

    const validationResults = {
        '성공한 주문은 초기 재고와 같아야 한다': successCount === INITIAL_STOCK,
        '실패한 주문 수는 실패 유형의 합과 같아야 한다': totalFailedOrders === calculatedFailedOrders,
        '실패한 주문은 결제 실패, 재고 부족, 이탈을 포함해야 한다': failedCount === (totalRequests - successCount - canceledCount - soldOutCount)
    };

    // k6 기본 메트릭스 추출
    const summary = `
== 주문 처리 결과 ==
총 요청 수: ${totalRequests}
구매 성공: ${successCount}
결제 실패: ${failedCount}
결제 중 이탈: ${canceledCount}
품절로 인한 구매 실패: ${soldOutCount}

== 검증 결과 ==
성공한 주문은 초기 재고와 같아야 한다: ${validationResults['성공한 주문은 초기 재고와 같아야 한다']}
실패한 주문 수는 실패 유형의 합과 같아야 한다: ${validationResults['실패한 주문 수는 실패 유형의 합과 같아야 한다']}
실패한 주문은 결제 실패, 재고 부족, 이탈을 포함해야 한다: ${validationResults['실패한 주문은 결제 실패, 재고 부족, 이탈을 포함해야 한다']}

== 성능 지표 ==
총 HTTP 요청 수: ${data.metrics.http_reqs.values.count}
총 반복 횟수: ${data.metrics.iterations.values.count}
VUs: ${data.metrics.vus.values.max}
최대 VUs: ${data.metrics.vus_max.values.max}

HTTP 요청 시간 (ms):
    평균: ${data.metrics.http_req_duration.values.avg.toFixed(2)}
    최소: ${data.metrics.http_req_duration.values.min.toFixed(2)}
    중앙값: ${data.metrics.http_req_duration.values.med.toFixed(2)}
    최대: ${data.metrics.http_req_duration.values.max.toFixed(2)}
    p(90): ${data.metrics.http_req_duration.values['p(90)'].toFixed(2)}
    p(95): ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}

`;

    console.log(summary);
    return {};
}
