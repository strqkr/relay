package com.gesmio.relay.web;

import com.gesmio.relay.domain.Organization;

import java.time.Instant;

public record OrganizationResponse(
        Long id,
        String name,
        String apiKey,
        Instant createdAt
) {
    public static OrganizationResponse from(Organization organization, String rawApiKey) {
        return new OrganizationResponse(organization.getId(), organization.getName(), rawApiKey, organization.getCreatedAt());
    }
}
