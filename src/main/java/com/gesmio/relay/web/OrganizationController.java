package com.gesmio.relay.web;

import com.gesmio.relay.domain.Organization;
import com.gesmio.relay.repository.OrganizationRepository;
import com.gesmio.relay.security.ApiKeyHasher;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/organizations")
public class OrganizationController {

    private final OrganizationRepository organizationRepository;
    private final ApiKeyHasher apiKeyHasher;

    public OrganizationController(OrganizationRepository organizationRepository, ApiKeyHasher apiKeyHasher) {
        this.organizationRepository = organizationRepository;
        this.apiKeyHasher = apiKeyHasher;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request) {
        String rawKey = apiKeyHasher.generateKey();
        Organization organization = new Organization(request.name(), apiKeyHasher.hash(rawKey));
        return OrganizationResponse.from(organizationRepository.save(organization), rawKey);
    }
}
