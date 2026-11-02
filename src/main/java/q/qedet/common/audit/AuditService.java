package q.qedet.common.audit;

import org.springframework.stereotype.Service;



import q.qedet.employee.entity.Employee;
import q.qedet.organization.entity.Organization;
import q.qedet.repo.AuditLogRepository;


@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(Organization organization, Employee actorEmployee, String action, String targetType, String targetId, String previousValue, String newValue) {
        auditLogRepository.save(new AuditLog(organization, actorEmployee, action, targetType, targetId, previousValue, newValue));
    }
}
