# Relay

A webhook delivery service — ingest events, sign and deliver them to registered endpoints, with automatic retries and a dashboard for replaying failed deliveries.

## What it does

- register endpoints and ingest events against them via a REST API
- signs every outbound payload with HMAC-SHA256 (`X-Relay-Signature` header) using a per-endpoint secret
- delivers events with a scheduled worker, retrying failed attempts with exponential backoff up to a configurable max
- per-endpoint rate limiting so a slow or misbehaving endpoint doesn't get flooded
- dashboard API to list deliveries by status and replay failed ones

## Stack

Spring Boot · Spring Data JPA · PostgreSQL · Bucket4j · Docker

## Running it

1. `docker compose up -d` — starts Postgres on `localhost:5432`
2. `./mvnw spring-boot:run` — starts the app on `localhost:8080`

## API

| Method | Path | Description |
|---|---|---|
| POST | `/endpoints` | register a new endpoint, returns a generated signing secret |
| GET | `/endpoints/{id}` | fetch an endpoint |
| POST | `/endpoints/{id}/events` | ingest an event for that endpoint (creates a pending delivery) |
| GET | `/deliveries?status=` | list deliveries, optionally filtered by status, paginated |
| POST | `/deliveries/{id}/replay` | reset a FAILED delivery back to PENDING with a fresh attempt budget |

## License

MIT — see [LICENSE](./LICENSE)
