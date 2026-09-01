package com.gesmio.relay.repository;

import com.gesmio.relay.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByApiKeyHash(String apiKeyHash);

    Optional<Organization> findByEmail(String email);

    boolean existsByEmail(String email);
}
