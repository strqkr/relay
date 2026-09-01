package com.gesmio.relay.web;

import com.gesmio.relay.domain.Endpoint;
import com.gesmio.relay.repository.EndpointRepository;
import com.gesmio.relay.repository.OrganizationRepository;
import com.gesmio.relay.security.ApiKeyHasher;
import com.gesmio.relay.support.OrganizationFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ApiKeyHasher apiKeyHasher;

    @Test
    void ingestsEventAndCreatesPendingDelivery() throws Exception {
        OrganizationFixtures.Seeded org = OrganizationFixtures.seed(organizationRepository, apiKeyHasher, "test-org");
        Endpoint endpoint = endpointRepository.save(new Endpoint(org.organization(), "orders-webhook", "https://example.com/hook", "s3cr3t"));

        mockMvc.perform(post("/endpoints/" + endpoint.getId() + "/events")
                        .header(HttpHeaders.AUTHORIZATION, org.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"order.created\",\"payload\":{\"orderId\":123}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").exists())
                .andExpect(jsonPath("$.deliveryId").exists())
                .andExpect(jsonPath("$.type").value("order.created"));
    }

    @Test
    void returnsNotFoundForUnknownEndpoint() throws Exception {
        OrganizationFixtures.Seeded org = OrganizationFixtures.seed(organizationRepository, apiKeyHasher, "test-org");

        mockMvc.perform(post("/endpoints/999999/events")
                        .header(HttpHeaders.AUTHORIZATION, org.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"order.created\",\"payload\":{}}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsNotFoundWhenEndpointBelongsToAnotherOrganization() throws Exception {
        OrganizationFixtures.Seeded owner = OrganizationFixtures.seed(organizationRepository, apiKeyHasher, "owner-org");
        OrganizationFixtures.Seeded other = OrganizationFixtures.seed(organizationRepository, apiKeyHasher, "other-org");
        Endpoint endpoint = endpointRepository.save(new Endpoint(owner.organization(), "orders-webhook", "https://example.com/hook", "s3cr3t"));

        mockMvc.perform(post("/endpoints/" + endpoint.getId() + "/events")
                        .header(HttpHeaders.AUTHORIZATION, other.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"order.created\",\"payload\":{}}"))
                .andExpect(status().isNotFound());
    }
}
