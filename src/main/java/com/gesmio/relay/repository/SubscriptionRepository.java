package com.gesmio.relay.repository;

import com.gesmio.relay.domain.Subscription;
import com.gesmio.relay.domain.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByTopic(Topic topic);
}
