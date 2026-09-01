package com.gesmio.relay.integration;

import com.gesmio.relay.delivery.DeliveryWorker;
import com.gesmio.relay.domain.Delivery;
import com.gesmio.relay.domain.DeliveryStatus;
import com.gesmio.relay.repository.DeliveryRepository;
import com.gesmio.relay.signing.HmacSigner;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static com.jayway.jsonpath.JsonPath.read;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the full webhook lifecycle through the real REST API and the delivery worker:
 * register an endpoint, ingest an event, let the worker deliver it, and confirm it shows up
 * signed and correctly in the dashboard.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebhookDeliveryFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeliveryWorker worker;

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

    private String createOrganizationAndGetAuthHeader() throws Exception {
        MvcResult createOrg = mockMvc.perform(post("/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"acme\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String apiKey = read(createOrg.getResponse().getContentAsString(), "$.apiKey");
        return "Bearer " + apiKey;
    }

    @Test
    void deliversIngestedEventEndToEndAndShowsUpInDashboard() throws Exception {
        String authHeader = createOrganizationAndGetAuthHeader();
        AtomicReference<String> receivedBody = new AtomicReference<>();
        AtomicReference<String> receivedSignature = new AtomicReference<>();
        server.createContext("/hook", exchange -> {
            receivedSignature.set(exchange.getRequestHeaders().getFirst(HmacSigner.SIGNATURE_HEADER));
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        MvcResult createEndpoint = mockMvc.perform(post("/endpoints")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"orders\",\"url\":\"http://localhost:" + port + "/hook\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String endpointBody = createEndpoint.getResponse().getContentAsString();
        Long endpointId = ((Number) read(endpointBody, "$.id")).longValue();
        String secret = read(endpointBody, "$.secret");

        MvcResult ingest = mockMvc.perform(post("/endpoints/" + endpointId + "/events")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"order.created\",\"payload\":{\"orderId\":42}}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long deliveryId = ((Number) read(ingest.getResponse().getContentAsString(), "$.deliveryId")).longValue();

        // simulate the scheduler waking up and processing the newly ingested, due delivery
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow();
        worker.attempt(delivery);

        delivery = deliveryRepository.findById(deliveryId).orElseThrow();
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(receivedBody.get()).isEqualTo(delivery.getEvent().getPayload());
        assertThat(receivedSignature.get()).isEqualTo(hmacSigner.sign(receivedBody.get(), secret));

        mockMvc.perform(get("/deliveries")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .param("status", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + deliveryId + ")]").exists());
    }

    @Test
    void replayedFailedDeliveryBecomesDueAgain() throws Exception {
        String authHeader = createOrganizationAndGetAuthHeader();
        server.createContext("/hook", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();

        MvcResult createEndpoint = mockMvc.perform(post("/endpoints")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"orders\",\"url\":\"http://localhost:" + port + "/hook\"}"))
                .andReturn();
        Long endpointId = ((Number) read(createEndpoint.getResponse().getContentAsString(), "$.id")).longValue();

        MvcResult ingest = mockMvc.perform(post("/endpoints/" + endpointId + "/events")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"order.created\",\"payload\":{}}"))
                .andReturn();
        Long deliveryId = ((Number) read(ingest.getResponse().getContentAsString(), "$.deliveryId")).longValue();

        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow();
        delivery.setAttemptCount(delivery.getMaxAttempts() - 1);
        deliveryRepository.save(delivery);
        worker.attempt(delivery);

        Delivery failed = deliveryRepository.findById(deliveryId).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(DeliveryStatus.FAILED);

        mockMvc.perform(post("/deliveries/" + deliveryId + "/replay")
                        .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.attemptCount").value(0));
    }
}
