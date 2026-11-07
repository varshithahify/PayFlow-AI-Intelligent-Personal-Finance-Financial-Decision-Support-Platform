import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const page0Duration = new Trend('page_0_duration');
const page50Duration = new Trend('page_50_duration');
const page90Duration = new Trend('page_90_duration');

export const options = {
    stages: [
        { duration: '20s', target: 2 },
        { duration: '40s', target: 5 },
        { duration: '20s', target: 0 },
    ],

    thresholds: {
        http_req_failed: ['rate<0.01'],
        page_0_duration: ['p(95)<1000'],
        page_50_duration: ['p(95)<1000'],
        page_90_duration: ['p(95)<1000'],
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
        JSON.stringify({ email, password }),
        {
            headers: {
                'Content-Type': 'application/json',
            },
        }
    );

    if (response.status !== 200) {
        throw new Error(
            `Authentication failed: HTTP ${response.status}`
        );
    }

    const token = response.json('token');

    if (!token) {
        throw new Error('JWT token was not returned');
    }

    return { token };
}

export default function (data) {
    const pages = [
        { number: 0, metric: page0Duration },
        { number: 50, metric: page50Duration },
        { number: 90, metric: page90Duration },
    ];

    for (const page of pages) {
        const response = http.get(
            `${BASE_URL}/api/transactions/page?page=${page.number}&size=10&sortBy=createdAt&direction=desc`,
            {
                headers: {
                    Authorization: `Bearer ${data.token}`,
                    Accept: 'application/json',
                },
                tags: {
                    name: `GET /api/transactions/page/${page.number}`,
                },
            }
        );

        page.metric.add(response.timings.duration);

        let body = null;

        try {
            body = response.json();
        } catch (error) {
            body = null;
        }

        check(response, {
            [`page ${page.number}: HTTP 200`]:
                (r) => r.status === 200,

            [`page ${page.number}: 10 records returned`]:
                () => body !== null &&
                    Array.isArray(body.content) &&
                    body.content.length === 10,

            [`page ${page.number}: correct page number`]:
                () => body !== null &&
                    body.number === page.number,
        });

        sleep(0.5);
    }
}
