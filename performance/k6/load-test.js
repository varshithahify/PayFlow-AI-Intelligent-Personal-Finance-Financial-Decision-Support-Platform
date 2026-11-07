import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 5 },
        { duration: '60s', target: 10 },
        { duration: '30s', target: 0 },
    ],

    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
    },
};

const BASE_URL = 'http://localhost:8081';

export default function () {

    const response = http.get(
        `${BASE_URL}/actuator/health`
    );

    check(response, {
        'status is 200': (r) => r.status === 200,
        'health status is UP': (r) =>
            r.body.includes('"status":"UP"'),
    });

    sleep(1);
}