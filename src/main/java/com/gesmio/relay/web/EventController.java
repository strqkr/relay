package com.gesmio.relay.web;

import com.gesmio.relay.domain.Delivery;
import com.gesmio.relay.domain.Endpoint;
import com.gesmio.relay.domain.Event;
import com.gesmio.relay.domain.Organization;
import com.gesmio.relay.repository.DeliveryRepository;
import com.gesmio.relay.repository.EndpointRepository;
import com.gesmio.relay.repository.EventRepository;
import com.gesmio.relay.security.ApiKeyAuthFilter;
import com.gesmio.relay.streams.DeliveryStreamPublisher;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/endpoints/{endpointId}/events")
public class EventController {

    private final EndpointRepository endpointRepository;
    private final EventRepository eventRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryStreamPublisher deliveryStreamPublisher;

    public EventController(EndpointRepository endpointRepository, EventRepository eventRepository,
                            DeliveryRepository deliveryRepository, DeliveryStreamPublisher deliveryStreamPublisher) {
        this.endpointRepository = endpointRepository;
        this.eventRepository = eventRepository;
        this.deliveryRepository = deliveryRepository;
        this.deliveryStreamPublisher = deliveryStreamPublisher;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse ingest(@RequestAttribute(ApiKeyAuthFilter.ORGANIZATION_ATTRIBUTE) Organization organization,
                                 @PathVariable Long endpointId, @Valid @RequestBody IngestEventRequest request) {
        Endpoint endpoint = endpointRepository.findByIdAndOrganization(endpointId, organization)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "endpoint not found"));

        Event event = eventRepository.save(new Event(endpoint, request.type(), request.payload().toString()));
        Delivery delivery = deliveryRepository.save(new Delivery(event));
        deliveryStreamPublisher.publish(delivery.getId());

        return EventResponse.from(event, delivery);
    }
}
