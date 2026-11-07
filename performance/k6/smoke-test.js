import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 1,
    iterations: 20,

    thresholds: {
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = 'http://localhost:8081';

export default function () {

    const response = http.get(
        `${BASE_URL}/actuator/health`
    );

    check(response, {
        'status is 200': (r) => r.status === 200,
        'response contains UP': (r) =>
            r.body.includes('"status":"UP"'),
    });

    sleep(1);
}