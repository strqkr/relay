package com.gesmio.relay.observability;

import com.gesmio.relay.delivery.DeliveryWorker;
import com.gesmio.relay.domain.Delivery;
import com.gesmio.relay.domain.Endpoint;
import com.gesmio.relay.domain.Event;
import com.gesmio.relay.domain.Organization;
import com.gesmio.relay.domain.Topic;
import com.gesmio.relay.repository.DeliveryRepository;
import com.gesmio.relay.repository.EndpointRepository;
import com.gesmio.relay.repository.EventRepository;
import com.gesmio.relay.repository.OrganizationRepository;
import com.gesmio.relay.repository.TopicRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Confirms /actuator/prometheus is publicly reachable (no API key required, unlike the
 * business endpoints) and reports the custom delivery metrics after an attempt.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
class PrometheusMetricsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeliveryWorker worker;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EndpointRepository endpointRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

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

    @Test
    void reportsDeliveryAttemptMetricsAfterAnAttempt() throws Exception {
        server.createContext("/hook", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        Organization organization = organizationRepository.save(new Organization("acme", "hash-" + System.nanoTime()));
        Endpoint endpoint = endpointRepository.save(new Endpoint(organization, "test", "http://localhost:" + port + "/hook", "s3cr3t"));
        Topic topic = topicRepository.save(new Topic(organization, "test.event"));
        Event event = eventRepository.save(new Event(topic, "{}"));
        Delivery delivery = deliveryRepository.save(new Delivery(event, endpoint));

        worker.attempt(delivery);

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("relay_delivery_attempts_total")));
    }
}
