package com.gesmio.relay.repository;

import com.gesmio.relay.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
