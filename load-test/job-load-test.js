import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const submitLatency = new Trend('job_submit_latency_ms');
const submitFailures = new Counter('job_submit_failures');

const API_BASE = __ENV.API_BASE || 'http://localhost:8080';
const JOB_TYPES = ['EMAIL', 'WEBHOOK', 'REPORT_GENERATION'];

// Usage:
//   k6 run load-test/job-load-test.js
//   k6 run --env VUS=20 --env DURATION=60s load-test/job-load-test.js
//
// This measures SUBMISSION throughput/latency only (how fast api-service accepts jobs).
// It does NOT measure end-to-end completion - for that, snapshot dashboard-service's
// /stats endpoint before and after the run (see load-test/README.md) and diff:
//   - succeeded count / test duration = actual completion throughput
//   - avgCompletionMillis = end-to-end dispatch-to-success latency
// The two numbers matter separately: submission throughput tells you how fast the API
// can take work in; completion throughput tells you how fast your workers can drain it.
// If submission throughput >> completion throughput, jobs are piling up in PENDING -
// that's the exact scenario the "scale worker instances" story is meant to fix.

export const options = {
    scenarios: {
        submit_jobs: {
            executor: 'constant-vus',
            vus: Number(__ENV.VUS || 10),
            duration: __ENV.DURATION || '30s',
        },
    },
    thresholds: {
        // fail the run loudly if api-service itself starts falling over under load
        http_req_failed: ['rate<0.01'],
        job_submit_latency_ms: ['p(95)<500'],
    },
};

export default function () {
    const jobType = JOB_TYPES[Math.floor(Math.random() * JOB_TYPES.length)];
    const payload = JSON.stringify({
        jobType,
        payload: { generatedBy: 'k6-load-test', at: new Date().toISOString() },
        priority: 5,
        idempotencyKey: uuidv4(),
    });

    const res = http.post(`${API_BASE}/jobs`, payload, {
        headers: { 'Content-Type': 'application/json' },
    });

    submitLatency.add(res.timings.duration);

    const ok = check(res, {
        'status is 200 or 201': (r) => r.status === 200 || r.status === 201,
    });
    if (!ok) submitFailures.add(1);

    sleep(0.1);
}
