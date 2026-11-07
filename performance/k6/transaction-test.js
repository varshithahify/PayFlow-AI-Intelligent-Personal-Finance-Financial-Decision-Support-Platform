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

export function setup() {
    const payload = JSON.stringify({
        email: email,
        password: password,
    });

    const response = http.post(
        `${BASE_URL}/auth/login`,
        payload,
        {
            headers: {
                'Content-Type': 'application/json',
            },
            tags: {
                name: 'POST /auth/login',
            },
        }
    );

    if (response.status !== 200) {
        throw new Error(
            `Authentication failed. HTTP status: ${response.status}`
        );
    }

    const token = response.json('token');

    if (!token) {
        throw new Error('JWT token was not returned');
    }

    return { token: token };
}

export default function (data) {
    const response = http.get(
        `${BASE_URL}/api/transactions/page?page=0&size=10&sortBy=createdAt&direction=desc`,
        {
            headers: {
                'Authorization': `Bearer ${data.token}`,
                'Accept': 'application/json',
            },
            tags: {
                name: 'GET /api/transactions/page',
            },
        }
    );

    let body = null;

    try {
        body = response.json();
    } catch (error) {
        body = null;
    }

    check(response, {
        'transaction API returns HTTP 200':
            (r) => r.status === 200,

        'transaction API returns JSON':
            (r) => r.headers['Content-Type'] &&
                r.headers['Content-Type'].includes('application/json'),

        'transaction response contains content array':
            () => body !== null && Array.isArray(body.content),

        'transaction page contains 10 records':
            () => body !== null &&
                Array.isArray(body.content) &&
                body.content.length === 10,

        'transaction response contains pagination metadata':
            () => body !== null &&
                typeof body.totalElements === 'number' &&
                typeof body.totalPages === 'number' &&
                typeof body.number === 'number' &&
                typeof body.size === 'number',

        'transaction page number is zero':
            () => body !== null && body.number === 0,

        'transaction page size is 10':
            () => body !== null && body.size === 10,

        'transaction records contain required fields':
            () => body !== null &&
                Array.isArray(body.content) &&
                body.content.every((transaction) =>
                    transaction.id !== null &&
                    transaction.amount !== null &&
                    transaction.currency &&
                    transaction.status
                ),
    });

    sleep(1);
}
