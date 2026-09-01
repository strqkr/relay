package com.gesmio.relay.repository;

import com.gesmio.relay.domain.Endpoint;
import com.gesmio.relay.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EndpointRepository extends JpaRepository<Endpoint, Long> {

    Optional<Endpoint> findByIdAndOrganization(Long id, Organization organization);
}
