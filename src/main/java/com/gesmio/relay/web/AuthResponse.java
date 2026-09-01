package com.gesmio.relay.web;

import com.gesmio.relay.domain.Organization;

public record AuthResponse(Long organizationId, String organizationName, String email, String apiKey) {

    public static AuthResponse from(Organization organization, String apiKey) {
        return new AuthResponse(organization.getId(), organization.getName(), organization.getEmail(), apiKey);
    }
}
