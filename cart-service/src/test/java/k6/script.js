import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '10s', target: 5 },  // 10초 동안 사용자를 5명으로 증가
        { duration: '1m', target: 20 }, // 1분 동안 20명의 사용자 유지
        { duration: '10s', target: 0 }, // 10초 동안 사용자 0명으로 감소
    ],
    thresholds: {
        'http_req_duration': ['p(95)<500'], // 95%의 요청이 500ms 이하로 응답해야 함
        'http_req_failed': ['rate<0.05'],   // 실패율이 5% 미만이어야 함
    },
};

const BASE_URL = 'http://localhost:8083';

// 부하 테스트 실행
export default function () {
    const endpoints = ['/test/case1', '/test/case2', '/test/case3'];

    // 각각의 엔드포인트를 랜덤으로 호출
    const endpoint = endpoints[Math.floor(Math.random() * endpoints.length)];
    const res = http.get(`${BASE_URL}${endpoint}`);

    // 응답 상태 코드 확인
    check(res, {
        'status is 200': (r) => r.status === 200,
        'status is 500': (r) => r.status === 500,
        'status is 503': (r) => r.status === 503,
    });

    // 1초 대기
    sleep(1);
}
