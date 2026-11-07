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
    const response = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({
            email: email,
            password: password,
        }),
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
    const pages = [0, 50, 90];

    for (const page of pages) {
        const response = http.get(
            `${BASE_URL}/api/transactions/page?page=${page}&size=10&sortBy=createdAt&direction=desc`,
            {
                headers: {
                    'Authorization': `Bearer ${data.token}`,
                    'Accept': 'application/json',
                },
                tags: {
                    name: `GET /api/transactions/page/${page}`,
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
            [`page ${page}: HTTP 200`]:
                (r) => r.status === 200,

            [`page ${page}: content contains 10 records`]:
                () => body !== null &&
                    Array.isArray(body.content) &&
                    body.content.length === 10,

            [`page ${page}: correct page number`]:
                () => body !== null &&
                    body.number === page,

            [`page ${page}: correct page size`]:
                () => body !== null &&
                    body.size === 10,
        });

        sleep(0.5);
    }
}
