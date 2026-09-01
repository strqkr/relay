package com.gesmio.relay.repository;

import com.gesmio.relay.domain.Organization;
import com.gesmio.relay.domain.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    Optional<Topic> findByIdAndOrganization(Long id, Organization organization);
}
