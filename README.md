# Relay

A multi-tenant webhook delivery service — publish events to topics, fan them out
to every subscribed endpoint, sign each payload, and retry failures automatically.
Includes a dashboard for managing endpoints/topics and replaying failed deliveries,
and a Node SDK for integrating from your own backend.

## What it does

- multi-tenant: each organization gets its own API key and only ever sees its own data
- publish/subscribe: create a `Topic`, subscribe verified `Endpoint`s to it, and every
  event published to that topic fans out to all of them
- endpoint ownership verification: a signed ping must succeed before an endpoint can be
  subscribed to anything, so events never get sent to a URL you don't control
- signs every outbound payload with HMAC-SHA256 (`X-Relay-Signature` header) using a
  per-endpoint secret
- delivers events two ways: a fast path via Redis Streams, plus a scheduled poller that
  retries failed attempts with exponential backoff up to a configurable max, so nothing
  is lost if the fast path misses it
- per-endpoint rate limiting (Redis-backed, consistent across instances)
- an audit log of every organization/endpoint/topic/delivery action
- Prometheus metrics at `/actuator/prometheus`
- a Next.js dashboard (`web/`) with real login/signup, backed by session cookies
  separate from the API key
- a Node client SDK (`sdk/`) for publishing events and verifying inbound webhooks

## Stack

Spring Boot · Spring Data JPA · PostgreSQL · Redis (rate limiting + Streams) ·
Micrometer/Prometheus · Next.js · Tailwind + shadcn/ui · Docker

## Running it

1. `docker compose up -d` — starts Postgres (`localhost:5432`) and Redis (`localhost:6379`)
2. `./mvnw spring-boot:run` — starts the API on `localhost:8080`
3. `cd web && npm install && npm run dev` — starts the dashboard on `localhost:3000`

## API

| Method | Path | Description |
|---|---|---|
| POST | `/organizations` | provision an org directly via the API, returns an API key |
| POST | `/auth/signup` | create an org with dashboard credentials (email/password), returns an API key and logs you in |
| POST | `/auth/login` | log in to the dashboard (session cookie) |
| POST | `/auth/logout` | end the dashboard session |
| GET | `/auth/me` | current session's organization |
| POST | `/endpoints` | register an endpoint, returns a generated signing secret |
| GET | `/endpoints` / `/endpoints/{id}` | list/fetch endpoints |
| POST | `/endpoints/{id}/verify` | ping the endpoint with a signed test payload; must succeed before it can be subscribed |
| POST | `/topics` | create a topic |
| GET | `/topics` / `/topics/{id}/subscriptions` | list topics / a topic's subscriptions |
| POST | `/topics/{id}/subscriptions` | subscribe a verified endpoint to a topic |
| POST | `/topics/{id}/events` | publish an event; fans out to every subscription |
| GET | `/deliveries?status=` | list deliveries, optionally filtered by status, paginated |
| POST | `/deliveries/{id}/replay` | reset a FAILED delivery back to PENDING with a fresh attempt budget |
| GET | `/audit-logs` | an organization's audit trail |

All endpoints except `/organizations`, `/auth/*` and `/actuator/*` require either an
`Authorization: Bearer relay_...` API key or a dashboard session cookie.

## Client SDK

See [`sdk/README.md`](./sdk/README.md) for publishing events and verifying webhooks from Node.

## License

MIT — see [LICENSE](./LICENSE)
