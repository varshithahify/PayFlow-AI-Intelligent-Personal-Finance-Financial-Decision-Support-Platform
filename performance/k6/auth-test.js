import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
    { duration: '20s', target: 2 },
    { duration: '40s', target: 5 },
    { duration: '20s', target: 0 },
],

    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1000'],
    },
};

const BASE_URL = 'http://localhost:8081';

const email = __ENV.LOGIN_EMAIL;
const password = __ENV.LOGIN_PASSWORD;

if (!email || !password) {
    throw new Error('LOGIN_EMAIL and LOGIN_PASSWORD are required');
}

export default function () {

    const payload = JSON.stringify({
        email: email,
        password: password,
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
        tags: {
            name: 'POST /auth/login',
        },
    };

    const response = http.post(
        `${BASE_URL}/auth/login`,
        payload,
        params
    );

    if (__VU === 1 && __ITER === 0) {
    console.log(`Login status: ${response.status}`);
    console.log(`Login response: ${response.body}`);
}

    let token = '';

    try {
        const body = response.json();
        token = body.token || '';
    } catch (error) {
        console.error('Invalid login response JSON');
    }

    check(response, {
        'login returns HTTP 200': (r) => r.status === 200,
        'JWT token is returned': () => token.length > 0,
    });

    sleep(1);
}