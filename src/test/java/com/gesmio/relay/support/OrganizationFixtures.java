package com.gesmio.relay.support;

import com.gesmio.relay.domain.Organization;
import com.gesmio.relay.repository.OrganizationRepository;
import com.gesmio.relay.security.ApiKeyHasher;

public final class OrganizationFixtures {

    private OrganizationFixtures() {
    }

    public record Seeded(Organization organization, String rawApiKey) {
        public String authorizationHeader() {
            return "Bearer " + rawApiKey;
        }
    }

    public static Seeded seed(OrganizationRepository organizationRepository, ApiKeyHasher apiKeyHasher, String name) {
        String rawKey = apiKeyHasher.generateKey();
        Organization organization = organizationRepository.save(new Organization(name, apiKeyHasher.hash(rawKey)));
        return new Seeded(organization, rawKey);
    }
}
