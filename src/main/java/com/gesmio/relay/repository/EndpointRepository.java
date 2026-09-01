package com.gesmio.relay.repository;

import com.gesmio.relay.domain.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EndpointRepository extends JpaRepository<Endpoint, Long> {
}
