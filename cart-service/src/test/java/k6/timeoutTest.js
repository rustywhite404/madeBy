import http from 'k6/http';
import { check, sleep } from 'k6';

/*
* productsService.getProductInfo(productInfoId)에서
* Thread.sleep으로 Timeout을 시켰다고 가정하고 테스트
*
* */
export const options = {
    vus: 10, // 동시 사용자 수
    duration: '30s', // 테스트 지속 시간
};

export default function () {
    const url = 'http://localhost:8083/api/cart/add'; // addProduct 호출
    const payload = JSON.stringify({
        productInfoId: 10, // 테스트용 상품 ID
        quantity: 1, // 테스트용 수량
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-User-Id': '1', // 테스트용 사용자 ID
            'X-User-Role': 'USER',
            'X-User-Enabled': 'true',
        },
    };

    const response = http.post(url, payload, params);

    // 응답 바디 출력 (디버깅용)
    console.log(response.body);

    // 응답 확인
    check(response, {
        'is status 200': (r) => r.status === 200,
        'is fallback triggered': (r) => {
            const responseBody = JSON.parse(r.body || '{}');
            return responseBody.message && responseBody.message.includes('Fallback');
        },
    });

    sleep(1); // 1초 대기
}
