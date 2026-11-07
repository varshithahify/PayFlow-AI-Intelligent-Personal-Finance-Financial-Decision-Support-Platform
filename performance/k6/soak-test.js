import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '1m', target: 20 },
        { duration: '10m', target: 20 },
        { duration: '1m', target: 0 },
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