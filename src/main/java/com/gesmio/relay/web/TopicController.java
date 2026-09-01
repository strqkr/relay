package com.gesmio.relay.web;

import com.gesmio.relay.audit.AuditLogService;
import com.gesmio.relay.domain.Delivery;
import com.gesmio.relay.domain.Endpoint;
import com.gesmio.relay.domain.Event;
import com.gesmio.relay.domain.Organization;
import com.gesmio.relay.domain.Subscription;
import com.gesmio.relay.domain.Topic;
import com.gesmio.relay.repository.DeliveryRepository;
import com.gesmio.relay.repository.EndpointRepository;
import com.gesmio.relay.repository.EventRepository;
import com.gesmio.relay.repository.SubscriptionRepository;
import com.gesmio.relay.repository.TopicRepository;
import com.gesmio.relay.security.ApiKeyAuthFilter;
import com.gesmio.relay.streams.DeliveryStreamPublisher;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/topics")
public class TopicController {

    private final TopicRepository topicRepository;
    private final EndpointRepository endpointRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final EventRepository eventRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryStreamPublisher deliveryStreamPublisher;
    private final AuditLogService auditLogService;

    public TopicController(TopicRepository topicRepository, EndpointRepository endpointRepository,
                            SubscriptionRepository subscriptionRepository, EventRepository eventRepository,
                            DeliveryRepository deliveryRepository, DeliveryStreamPublisher deliveryStreamPublisher,
                            AuditLogService auditLogService) {
        this.topicRepository = topicRepository;
        this.endpointRepository = endpointRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.eventRepository = eventRepository;
        this.deliveryRepository = deliveryRepository;
        this.deliveryStreamPublisher = deliveryStreamPublisher;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TopicResponse create(@RequestAttribute(ApiKeyAuthFilter.ORGANIZATION_ATTRIBUTE) Organization organization,
                                 @Valid @RequestBody CreateTopicRequest request) {
        Topic topic = topicRepository.save(new Topic(organization, request.name()));
        auditLogService.record(organization, "topic.created", "name=" + request.name());
        return TopicResponse.from(topic);
    }

    @PostMapping("/{topicId}/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public SubscriptionResponse subscribe(@RequestAttribute(ApiKeyAuthFilter.ORGANIZATION_ATTRIBUTE) Organization organization,
                                           @PathVariable Long topicId,
                                           @Valid @RequestBody CreateSubscriptionRequest request) {
        Topic topic = topicRepository.findByIdAndOrganization(topicId, organization)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "topic not found"));
        Endpoint endpoint = endpointRepository.findByIdAndOrganization(request.endpointId(), organization)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "endpoint not found"));
        if (!endpoint.isVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "endpoint must be verified before it can be subscribed to a topic");
        }

        Subscription subscription = subscriptionRepository.save(new Subscription(topic, endpoint));
        auditLogService.record(organization, "subscription.created", "topic=" + topic.getName() + ", endpointId=" + endpoint.getId());
        return SubscriptionResponse.from(subscription);
    }

    @PostMapping("/{topicId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public EventResponse ingest(@RequestAttribute(ApiKeyAuthFilter.ORGANIZATION_ATTRIBUTE) Organization organization,
                                 @PathVariable Long topicId,
                                 @Valid @RequestBody IngestEventRequest request) {
        Topic topic = topicRepository.findByIdAndOrganization(topicId, organization)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "topic not found"));

        Event event = eventRepository.save(new Event(topic, request.payload().toString()));

        List<Subscription> subscriptions = subscriptionRepository.findByTopic(topic);
        List<Delivery> deliveries = subscriptions.stream()
                .map(subscription -> deliveryRepository.save(new Delivery(event, subscription.getEndpoint())))
                .toList();
        deliveries.forEach(delivery -> deliveryStreamPublisher.publish(delivery.getId()));

        return EventResponse.from(event, deliveries);
    }
}
