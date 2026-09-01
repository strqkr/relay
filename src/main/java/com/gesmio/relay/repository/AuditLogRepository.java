package com.gesmio.relay.repository;

import com.gesmio.relay.domain.AuditLog;
import com.gesmio.relay.domain.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByOrganization(Organization organization, Pageable pageable);
}
