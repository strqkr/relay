package com.gesmio.relay.web;

import com.gesmio.relay.domain.Delivery;
import com.gesmio.relay.domain.Event;

import java.util.List;

public record EventResponse(Long eventId, List<Long> deliveryIds) {
    public static EventResponse from(Event event, List<Delivery> deliveries) {
        return new EventResponse(event.getId(), deliveries.stream().map(Delivery::getId).toList());
    }
}
