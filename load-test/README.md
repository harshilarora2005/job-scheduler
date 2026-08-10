# Load testing job-scheduler

Two things get measured, and they're not the same number:

- **Submission throughput/latency** — how fast `api-service` accepts `POST /jobs` requests.
  This is what `job-load-test.js` measures directly via k6.
- **Completion throughput/latency** — how fast jobs actually get dispatched, claimed by a
  worker, and marked `SUCCEEDED`. This comes from `dashboard-service`'s `GET /stats`, which
  is fed entirely by the `job.events` Kafka stream (see main README / architecture notes).

The interesting story is the *gap* between these two: if submission outpaces completion,
jobs pile up as `PENDING` and never get worked. Scaling worker instances (registered via
Eureka, no manual port wiring needed) is what should close that gap — that's the number to
capture for the resume.

## Prerequisites

- k6 installed (`brew install k6` on macOS)
- Full stack running: `docker compose -f infra/docker-compose.yml up -d`, then
  eureka-server, api-service, scheduler-service, worker-service (N instances),
  dashboard-service, all via `./mvnw spring-boot:run`

## Running a test

```bash
# baseline: default 10 VUs for 30s
k6 run load-test/job-load-test.js

# heavier / longer run
k6 run --env VUS=30 --env DURATION=60s load-test/job-load-test.js
```

## Methodology: capturing real before/after numbers

1. **Snapshot `/stats` before the run**: `curl localhost:8083/stats > before.json`
2. Start **1** worker-service instance only.
3. Run the k6 script, note the k6 summary (submission p95, throughput) at the end.
4. Wait until `/stats`'s `dispatched` count stops growing relative to `succeeded`+`failed`+`dead`
   (i.e. the backlog has drained) — this can take a while with only 1 worker.
5. **Snapshot `/stats` after**: `curl localhost:8083/stats > after-1-worker.json`
6. Compute: `(succeeded_after - succeeded_before) / wall_clock_seconds_to_drain` = completion
   throughput at 1 worker. `avgCompletionMillis` in the snapshot gives you the latency number
   directly.
7. Repeat steps 2-6 with 3 workers, then 5 workers (just run `./mvnw spring-boot:run` in
   worker-service again in new terminals — each grabs a random port and registers itself
   separately in Eureka; confirm the count at `http://localhost:8761`).
8. Compare completion throughput and `avgCompletionMillis` across 1 / 3 / 5 workers.

That comparison — sustained jobs/sec and p95-equivalent completion latency at each worker
count — is the concrete, non-invented metric this project was missing on the resume.

## Notes

- `submit_failures` and `http_req_failed` thresholds will fail the k6 run if api-service
  itself starts erroring under load — worth noting as a finding in its own right if it
  happens (e.g. HikariCP pool exhaustion under high VUs is a common, legitimate result to
  report and explain).
- This script only exercises `POST /jobs`. It intentionally does not attempt to drive load
  against dashboard-service's `/stats` endpoint itself, since that's the observation tool,
  not the thing under test.
