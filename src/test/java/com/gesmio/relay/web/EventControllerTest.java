package com.gesmio.relay.web;

import com.gesmio.relay.domain.Endpoint;
import com.gesmio.relay.repository.EndpointRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EndpointRepository endpointRepository;

    @Test
    void ingestsEventAndCreatesPendingDelivery() throws Exception {
        Endpoint endpoint = endpointRepository.save(new Endpoint("orders-webhook", "https://example.com/hook", "s3cr3t"));

        mockMvc.perform(post("/endpoints/" + endpoint.getId() + "/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"order.created\",\"payload\":{\"orderId\":123}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").exists())
                .andExpect(jsonPath("$.deliveryId").exists())
                .andExpect(jsonPath("$.type").value("order.created"));
    }

    @Test
    void returnsNotFoundForUnknownEndpoint() throws Exception {
        mockMvc.perform(post("/endpoints/999999/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"order.created\",\"payload\":{}}"))
                .andExpect(status().isNotFound());
    }
}
