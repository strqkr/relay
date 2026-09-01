# @gesmio/relay-sdk

Node.js client for the relay webhook delivery API.

## Usage

```ts
import { RelayClient } from "@gesmio/relay-sdk";

const relay = new RelayClient({ apiKey: process.env.RELAY_API_KEY! });

const topic = await relay.createTopic("order.created");
await relay.publish(topic.id, { orderId: 42 });
```

## Verifying inbound webhooks

```ts
import { verifyWebhookSignature } from "@gesmio/relay-sdk";

const signature = req.headers["x-relay-signature"];
if (!verifyWebhookSignature(rawBody, signature, endpointSecret)) {
  return res.status(401).end();
}
```

`rawBody` must be the exact bytes relay signed - decode it as UTF-8 text, not the
already-parsed JSON object, or the signature will never match.

## Development

```
npm install
npm test
npm run build
```
