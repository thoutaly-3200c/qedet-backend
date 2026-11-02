package q.qedet.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.common.audit.AuditLog;


import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
