package com.gesmio.relay.audit;

import com.gesmio.relay.domain.AuditLog;
import com.gesmio.relay.domain.Organization;
import com.gesmio.relay.repository.AuditLogRepository;
import org.springframework.stereotype.Component;

@Component
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(Organization organization, String action, String details) {
        auditLogRepository.save(new AuditLog(organization, action, details));
    }
}
