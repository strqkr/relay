package com.gesmio.relay.web;

import com.gesmio.relay.domain.Delivery;
import com.gesmio.relay.domain.DeliveryStatus;
import com.gesmio.relay.domain.Endpoint;
import com.gesmio.relay.domain.Event;
import com.gesmio.relay.repository.DeliveryRepository;
import com.gesmio.relay.repository.EndpointRepository;
import com.gesmio.relay.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EndpointRepository endpointRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    private Delivery seed(DeliveryStatus status) {
        Endpoint endpoint = endpointRepository.save(new Endpoint("orders-webhook", "https://example.com/hook", "s3cr3t"));
        Event event = eventRepository.save(new Event(endpoint, "order.created", "{}"));
        Delivery delivery = new Delivery(event);
        delivery.setStatus(status);
        return deliveryRepository.save(delivery);
    }

    @Test
    void listsDeliveriesFilteredByStatus() throws Exception {
        seed(DeliveryStatus.SUCCESS);
        Delivery failed = seed(DeliveryStatus.FAILED);

        mockMvc.perform(get("/deliveries").param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(failed.getId()))
                .andExpect(jsonPath("$.content[0].status").value("FAILED"));
    }

    @Test
    void replayResetsFailedDeliveryToPending() throws Exception {
        Delivery failed = seed(DeliveryStatus.FAILED);
        failed.setAttemptCount(failed.getMaxAttempts());
        failed.setNextAttemptAt(Instant.now().plus(1, ChronoUnit.HOURS));
        deliveryRepository.save(failed);

        mockMvc.perform(post("/deliveries/" + failed.getId() + "/replay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.attemptCount").value(0));
    }

    @Test
    void rejectsReplayOfNonFailedDelivery() throws Exception {
        Delivery pending = seed(DeliveryStatus.PENDING);

        mockMvc.perform(post("/deliveries/" + pending.getId() + "/replay"))
                .andExpect(status().isConflict());
    }

    @Test
    void returnsNotFoundForUnknownDelivery() throws Exception {
        mockMvc.perform(post("/deliveries/999999/replay"))
                .andExpect(status().isNotFound());
    }
}
