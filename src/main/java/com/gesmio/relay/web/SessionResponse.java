package com.gesmio.relay.web;

import com.gesmio.relay.domain.Organization;

public record SessionResponse(Long organizationId, String organizationName, String email) {

    public static SessionResponse from(Organization organization) {
        return new SessionResponse(organization.getId(), organization.getName(), organization.getEmail());
    }
}
