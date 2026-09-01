package com.gesmio.relay.web;

import com.gesmio.relay.domain.Delivery;
import com.gesmio.relay.domain.DeliveryStatus;

import java.time.Instant;

public record DeliveryResponse(
        Long id,
        Long eventId,
        Long endpointId,
        String eventType,
        DeliveryStatus status,
        int attemptCount,
        int maxAttempts,
        Instant nextAttemptAt,
        Instant lastAttemptAt,
        Integer lastResponseStatus,
        Instant createdAt
) {
    public static DeliveryResponse from(Delivery delivery) {
        return new DeliveryResponse(
                delivery.getId(),
                delivery.getEvent().getId(),
                delivery.getEvent().getEndpoint().getId(),
                delivery.getEvent().getType(),
                delivery.getStatus(),
                delivery.getAttemptCount(),
                delivery.getMaxAttempts(),
                delivery.getNextAttemptAt(),
                delivery.getLastAttemptAt(),
                delivery.getLastResponseStatus(),
                delivery.getCreatedAt()
        );
    }
}
