package com.gesmio.relay.web;

import com.gesmio.relay.domain.Endpoint;

import java.time.Instant;

public record EndpointResponse(
        Long id,
        String name,
        String url,
        String secret,
        int rateLimitPerSecond,
        Instant createdAt
) {
    public static EndpointResponse from(Endpoint endpoint) {
        return new EndpointResponse(
                endpoint.getId(),
                endpoint.getName(),
                endpoint.getUrl(),
                endpoint.getSecret(),
                endpoint.getRateLimitPerSecond(),
                endpoint.getCreatedAt()
        );
    }
}
