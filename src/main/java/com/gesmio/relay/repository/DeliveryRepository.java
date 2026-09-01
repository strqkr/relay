package com.gesmio.relay.repository;

import com.gesmio.relay.domain.Delivery;
import com.gesmio.relay.domain.DeliveryStatus;
import com.gesmio.relay.domain.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Page<Delivery> findByStatus(DeliveryStatus status, Pageable pageable);

    List<Delivery> findByStatusAndNextAttemptAtLessThanEqual(DeliveryStatus status, Instant now);

    Page<Delivery> findByEndpoint_OrganizationAndStatus(Organization organization, DeliveryStatus status, Pageable pageable);

    Page<Delivery> findByEndpoint_Organization(Organization organization, Pageable pageable);

    Optional<Delivery> findByIdAndEndpoint_Organization(Long id, Organization organization);
}
