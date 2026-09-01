package com.gesmio.relay.repository;

import com.gesmio.relay.domain.Delivery;
import com.gesmio.relay.domain.DeliveryStatus;
import com.gesmio.relay.domain.Endpoint;
import com.gesmio.relay.domain.Event;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DeliveryRepositoryTest {

    @Autowired
    private EndpointRepository endpointRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Test
    void findsDueDeliveriesByStatusAndNextAttemptAt() {
        Endpoint endpoint = endpointRepository.save(new Endpoint("orders-webhook", "https://example.com/hook", "s3cr3t"));
        Event event = eventRepository.save(new Event(endpoint, "order.created", "{}"));

        Delivery due = new Delivery(event);
        due.setNextAttemptAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        deliveryRepository.save(due);

        Delivery notYetDue = new Delivery(event);
        notYetDue.setNextAttemptAt(Instant.now().plus(1, ChronoUnit.HOURS));
        deliveryRepository.save(notYetDue);

        List<Delivery> result = deliveryRepository.findByStatusAndNextAttemptAtLessThanEqual(DeliveryStatus.PENDING, Instant.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(due.getId());
    }

    @Test
    void findsByStatusPaginated() {
        Endpoint endpoint = endpointRepository.save(new Endpoint("orders-webhook", "https://example.com/hook", "s3cr3t"));
        Event event = eventRepository.save(new Event(endpoint, "order.created", "{}"));

        Delivery failed = new Delivery(event);
        failed.setStatus(DeliveryStatus.FAILED);
        deliveryRepository.save(failed);

        Delivery pending = new Delivery(event);
        deliveryRepository.save(pending);

        assertThat(deliveryRepository.findByStatus(DeliveryStatus.FAILED, PageRequest.of(0, 10)).getContent())
                .extracting(Delivery::getId)
                .containsExactly(failed.getId());
    }
}
