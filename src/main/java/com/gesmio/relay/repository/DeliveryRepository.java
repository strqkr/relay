package com.gesmio.relay.repository;

import com.gesmio.relay.domain.Delivery;
import com.gesmio.relay.domain.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Page<Delivery> findByStatus(DeliveryStatus status, Pageable pageable);

    List<Delivery> findByStatusAndNextAttemptAtLessThanEqual(DeliveryStatus status, Instant now);
}
