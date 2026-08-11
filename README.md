# job-scheduler

A distributed job scheduling system built with Spring Boot, demonstrating horizontal scaling, event-driven architecture, and real-time observability. Jobs are dispatched via RabbitMQ, safely claimed across concurrent workers using PostgreSQL row-level locking, and tracked end-to-end through a Kafka event stream that feeds a live analytics dashboard.

<!-- Add a high-level architecture diagram here -->
<!-- ![Architecture](docs/images/architecture.png) -->

## Why this exists

Most "job scheduler" toy projects stop at a single service polling a database. This one is built to answer a harder question: what happens when you need to scale it? The project explores that directly — multiple worker instances registered via Eureka, RabbitMQ listener concurrency tuning, and load-tested, measured results showing how throughput scales as workers are added.

## Architecture

The system is split into four independently deployable Spring Boot services, plus a service registry:

- **eureka-server** — service registry. All other services register here and discover each other by name instead of hardcoded host/port.
- **api-service** — accepts job submissions (`POST /jobs`), validates and persists them to PostgreSQL. Supports idempotency keys to prevent duplicate job creation.
- **scheduler-service** — polls for due jobs, marks them dispatched, and pushes them onto a RabbitMQ queue to wake up workers. Also reclaims jobs whose lease expired (a worker died mid-execution) and returns them to the pending pool.
- **worker-service** — claims jobs using `SELECT ... FOR UPDATE SKIP LOCKED`, so multiple concurrent threads across multiple worker instances can pull from the same queue without double-processing a job. Executes the job, heartbeats while running, and marks it succeeded, failed (with exponential backoff retry), or dead-lettered after max attempts.
- **dashboard-service** — consumes a Kafka event stream (`job.events`) published by scheduler-service and worker-service on every state transition, and builds real-time statistics entirely from that stream rather than querying PostgreSQL. Serves both a JSON `/stats` endpoint and a live-updating dashboard UI.

<!-- Add a service diagram or sequence diagram here -->
<!-- ![Service flow](docs/images/service-flow.png) -->

### Why two message systems

RabbitMQ and Kafka are used deliberately for different jobs, not interchangeably:

- **RabbitMQ** is the work queue. A message is consumed once and gone — appropriate for "wake up a worker to do this job."
- **Kafka** is the event log. Every state transition is retained and can be replayed or read by multiple independent consumers — appropriate for analytics, where dashboard-service needs to reconstruct system-wide statistics without querying the transactional database.

### Concurrency and correctness

- **SKIP LOCKED job claiming**: the core correctness guarantee. Multiple worker threads, across multiple worker instances, can attempt to claim jobs concurrently. PostgreSQL's `FOR UPDATE SKIP LOCKED` ensures a job already being examined by one transaction is invisible to others, rather than making them wait or error.
- **Lease-based execution**: a claimed job carries an expiring lease. If a worker dies mid-execution, scheduler-service reclaims the job once the lease expires, rather than losing it.
- **Idempotency keys**: job submission accepts a client-supplied idempotency key, preventing duplicate job creation on retried requests.
- **Configurable listener concurrency**: each worker instance runs multiple concurrent RabbitMQ listener threads (`worker.rabbit.concurrency`), independently safe due to the SKIP LOCKED guarantee above.

## Scaling and load testing

Worker instances register with Eureka and can be scaled horizontally — no hardcoded ports or manual wiring required. This was measured directly with [k6](https://k6.io), scaling from 1 to 5 worker instances and comparing sustained job completion throughput and average completion latency:

| Worker instances | Completion throughput | Avg completion latency |
|---|---:|---:|
| 1 | 2.15 jobs/sec | ~100.0s |
| 3 | 6.45 jobs/sec | ~32.8s |
| 5 | 10.75 jobs/sec | ~19.1s |

Throughput scales near-linearly with worker count. Full methodology, including how to reproduce these numbers, is documented in [`load-test/README.md`](load-test/README.md).

<!-- Add throughput/latency chart screenshots here -->
<!-- ![Load test results](docs/images/load-test-results.png) -->

## Live dashboard

`dashboard-service` serves a live-updating dashboard (React, no build step required) showing real-time job counts, completion throughput, and latency, sourced entirely from the Kafka event stream. It also includes a built-in control to fire a test load of jobs directly from the browser.

<!-- Add a dashboard screenshot or short GIF here -->
<!-- ![Live dashboard](docs/images/dashboard.png) -->

Once the stack is running, open `http://localhost:8083`.

## Tech stack

Spring Boot, PostgreSQL, RabbitMQ, Apache Kafka, Netflix Eureka, React (CDN, no build step), k6, Docker Compose.

## Running locally

### Prerequisites

- Java 21+
- Maven
- Docker (for PostgreSQL, RabbitMQ, and Kafka)

### 1. Start infrastructure

```bash
docker compose -f infra/docker-compose.yml up -d
```

This brings up PostgreSQL, RabbitMQ, and a single-node Kafka broker (KRaft mode, no Zookeeper needed).

### 2. Start the services

Each service is run independently. In separate terminals:

```bash
cd eureka-server && ./mvnw spring-boot:run
cd api-service && ./mvnw spring-boot:run
cd scheduler-service && ./mvnw spring-boot:run
cd worker-service && ./mvnw spring-boot:run
cd dashboard-service && ./mvnw spring-boot:run
```

Confirm all services are registered at `http://localhost:8761`.

To scale workers, run `worker-service` again in additional terminals — each instance binds to a random free port and registers itself independently:

```bash
cd worker-service && ./mvnw spring-boot:run
```

### 3. Submit a job

```bash
curl -X POST localhost:8080/jobs \
  -H "Content-Type: application/json" \
  -d '{"jobType":"EMAIL","payload":{"to":"test@example.com"}}'
```

### 4. Watch it happen

- Live dashboard: `http://localhost:8083`
- Raw stats JSON: `curl localhost:8083/stats`
- Eureka registry: `http://localhost:8761`

## Load testing

See [`load-test/README.md`](load-test/README.md) for the k6 script and the methodology used to produce the throughput numbers above.

## Project structure

```
job-scheduler/
├── eureka-server/       service registry
├── api-service/         job submission API
├── scheduler-service/   dispatch and lease reclamation
├── worker-service/      job execution and claiming
├── dashboard-service/   real-time stats and live dashboard UI
├── load-test/           k6 script and load-test methodology
└── infra/                docker-compose for local infrastructure
```