package com.gesmio.relay.seed;

import com.gesmio.relay.domain.DeliveryStatus;
import com.gesmio.relay.domain.Organization;
import com.gesmio.relay.repository.DeliveryRepository;
import com.gesmio.relay.repository.EndpointRepository;
import com.gesmio.relay.repository.OrganizationRepository;
import com.gesmio.relay.repository.SubscriptionRepository;
import com.gesmio.relay.repository.TopicRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("seed")
class SeedDataRunnerTest {

    @Autowired
    private SeedDataRunner seedDataRunner;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EndpointRepository endpointRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    // The runner already ran once as part of context startup (it's an ApplicationRunner);
    // these assertions exercise that real run rather than invoking it again themselves.

    @Test
    void seedsTwoOrganizationsWithDashboardCredentials() {
        Organization acme = organizationRepository.findByEmail("owner@acme.dev").orElseThrow();
        Organization globex = organizationRepository.findByEmail("owner@globex.dev").orElseThrow();

        assertThat(acme.getName()).isEqualTo("Acme Corp");
        assertThat(acme.hasDashboardCredentials()).isTrue();
        assertThat(globex.getName()).isEqualTo("Globex Corp");
        assertThat(globex.hasDashboardCredentials()).isTrue();
    }

    @Test
    void acmeHasEndpointsInBothVerificationStates() {
        Organization acme = organizationRepository.findByEmail("owner@acme.dev").orElseThrow();

        var endpoints = endpointRepository.findByOrganization(acme);

        assertThat(endpoints).hasSize(3);
        assertThat(endpoints).filteredOn(e -> e.isVerified()).hasSize(2);
        assertThat(endpoints).filteredOn(e -> !e.isVerified()).hasSize(1);
    }

    @Test
    void acmeHasTopicsAndSubscriptions() {
        Organization acme = organizationRepository.findByEmail("owner@acme.dev").orElseThrow();

        assertThat(topicRepository.findByOrganization(acme)).hasSize(3);

        var orderCreated = topicRepository.findByOrganization(acme).stream()
                .filter(t -> t.getName().equals("order.created"))
                .findFirst().orElseThrow();
        assertThat(subscriptionRepository.findByTopic(orderCreated)).hasSize(1);
    }

    @Test
    void deliveriesCoverAllThreeStatuses() {
        Organization acme = organizationRepository.findByEmail("owner@acme.dev").orElseThrow();

        var deliveries = deliveryRepository
                .findByEndpoint_Organization(acme, org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        assertThat(deliveries).extracting(d -> d.getStatus())
                .contains(DeliveryStatus.SUCCESS, DeliveryStatus.FAILED, DeliveryStatus.PENDING);
    }

    @Test
    void runningAgainIsIdempotent() {
        long organizationsBefore = organizationRepository.count();

        seedDataRunner.run(null);

        assertThat(organizationRepository.count()).isEqualTo(organizationsBefore);
    }
}
