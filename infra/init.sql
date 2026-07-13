CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,
    job_type VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority SMALLINT NOT NULL DEFAULT 5,
    next_run_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    cron_expression VARCHAR(100),
    is_recurring BOOLEAN NOT NULL DEFAULT false,
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    last_error TEXT,
    lease_owner VARCHAR(100),
    lease_expires_at TIMESTAMPTZ,
    last_heartbeat_at TIMESTAMPTZ,
    idempotency_key VARCHAR(255) UNIQUE,
    job_hash VARCHAR(64),
    result JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_jobs_status_next_run
ON jobs (status, next_run_time);

CREATE INDEX idx_jobs_lease_expiry
ON jobs (status, lease_expires_at)
WHERE status = 'RUNNING';

CREATE INDEX idx_jobs_priority_order
ON jobs (priority ASC, next_run_time ASC)
WHERE status = 'PENDING';

CREATE TABLE dead_letter_jobs (
    id BIGSERIAL PRIMARY KEY,
    original_job_id BIGINT NOT NULL,
    job_type VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    attempt_count INT NOT NULL,
    last_error TEXT,
    moved_to_dlq_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE workers (
    worker_id VARCHAR(100) PRIMARY KEY,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_heartbeat_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    jobs_completed INT NOT NULL DEFAULT 0,
    jobs_failed INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ALIVE'
);

INSERT INTO jobs (job_type, payload, priority, next_run_time)
VALUES
    ('EMAIL', '{"to":"test@example.com","subject":"hello"}', 5, now()),
    ('WEBHOOK', '{"url":"https://httpbin.org/post","body":{}}', 3, now());