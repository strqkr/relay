package com.gesmio.relay.web;

import com.gesmio.relay.domain.Delivery;
import com.gesmio.relay.domain.Event;

public record EventResponse(
        Long eventId,
        Long deliveryId,
        String type
) {
    public static EventResponse from(Event event, Delivery delivery) {
        return new EventResponse(event.getId(), delivery.getId(), event.getType());
    }
}
