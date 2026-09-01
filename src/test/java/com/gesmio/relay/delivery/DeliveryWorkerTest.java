package com.gesmio.relay.delivery;

import com.gesmio.relay.domain.Delivery;
import com.gesmio.relay.domain.DeliveryStatus;
import com.gesmio.relay.domain.Endpoint;
import com.gesmio.relay.domain.Event;
import com.gesmio.relay.repository.DeliveryRepository;
import com.gesmio.relay.repository.EndpointRepository;
import com.gesmio.relay.repository.EventRepository;
import com.gesmio.relay.signing.HmacSigner;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DeliveryWorkerTest {

    @Autowired
    private DeliveryWorker worker;

    @Autowired
    private EndpointRepository endpointRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private HmacSigner hmacSigner;

    private HttpServer server;
    private int port;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private Delivery seedDelivery(String path, String secret) {
        Endpoint endpoint = endpointRepository.save(new Endpoint("test", "http://localhost:" + port + path, secret));
        Event event = eventRepository.save(new Event(endpoint, "test.event", "{\"a\":1}"));
        return deliveryRepository.save(new Delivery(event));
    }

    private Delivery seedDeliveryWithRateLimit(String path, String secret, int ratePerSecond) {
        Endpoint endpoint = new Endpoint("test", "http://localhost:" + port + path, secret);
        endpoint.setRateLimitPerSecond(ratePerSecond);
        endpoint = endpointRepository.save(endpoint);
        Event event = eventRepository.save(new Event(endpoint, "test.event", "{\"a\":1}"));
        return deliveryRepository.save(new Delivery(event));
    }

    @Test
    void marksDeliverySuccessAndSendsValidSignatureWhenEndpointReturns2xx() throws Exception {
        AtomicReference<String> receivedSignature = new AtomicReference<>();
        server.createContext("/hook", exchange -> {
            receivedSignature.set(exchange.getRequestHeaders().getFirst(HmacSigner.SIGNATURE_HEADER));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        Delivery delivery = seedDelivery("/hook", "s3cr3t");
        worker.attempt(delivery);

        Delivery reloaded = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(reloaded.getLastResponseStatus()).isEqualTo(200);
        assertThat(reloaded.getAttemptCount()).isEqualTo(1);
        assertThat(receivedSignature.get()).isEqualTo(hmacSigner.sign("{\"a\":1}", "s3cr3t"));
    }

    @Test
    void schedulesRetryWithBackoffWhenEndpointReturns5xx() throws Exception {
        server.createContext("/hook", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();

        Delivery delivery = seedDelivery("/hook", "s3cr3t");
        worker.attempt(delivery);

        Delivery reloaded = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(reloaded.getAttemptCount()).isEqualTo(1);
        assertThat(reloaded.getLastResponseStatus()).isEqualTo(500);
        assertThat(reloaded.getNextAttemptAt()).isAfter(Instant.now());
    }

    @Test
    void marksDeliveryFailedAfterExhaustingMaxAttempts() throws Exception {
        server.createContext("/hook", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();

        Delivery delivery = seedDelivery("/hook", "s3cr3t");
        delivery.setAttemptCount(delivery.getMaxAttempts() - 1);
        deliveryRepository.save(delivery);

        worker.attempt(delivery);

        Delivery reloaded = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(reloaded.getAttemptCount()).isEqualTo(reloaded.getMaxAttempts());
    }

    @Test
    void schedulesRetryWhenEndpointIsUnreachable() {
        // server is deliberately never started, so the port is not listening
        Delivery delivery = seedDelivery("/hook", "s3cr3t");

        worker.attempt(delivery);

        Delivery reloaded = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(reloaded.getLastResponseStatus()).isNull();
    }

    @Test
    void skipsAttemptWithoutHittingEndpointWhenRateLimited() throws Exception {
        AtomicReference<Integer> requestCount = new AtomicReference<>(0);
        server.createContext("/hook", exchange -> {
            requestCount.updateAndGet(count -> count + 1);
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        Delivery delivery = seedDeliveryWithRateLimit("/hook", "s3cr3t", 1);

        worker.attempt(delivery); // consumes the single token for this endpoint, succeeds
        worker.attempt(delivery); // should be rate-limited, no HTTP call made

        assertThat(requestCount.get()).isEqualTo(1);

        Delivery reloaded = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(reloaded.getAttemptCount()).isEqualTo(1);
    }
}
