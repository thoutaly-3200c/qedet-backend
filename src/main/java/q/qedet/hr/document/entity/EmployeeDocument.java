package q.qedet.hr.document.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.hr.document.model.EmployeeDocumentStatus;
import q.qedet.organization.entity.Organization;
import java.time.*;

@Entity @Table(name = "hr_employee_documents") @Getter @Setter @NoArgsConstructor
public class EmployeeDocument extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @ManyToOne(optional = false) @JoinColumn(name = "employee_id", nullable = false) private Employee employee;
    @Column(nullable = false) private String documentType;
    @Column(nullable = false) private String fileName;
    @Column(nullable = false, length = 2000) private String storageKey;
    private LocalDate expiresOn;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private EmployeeDocumentStatus status = EmployeeDocumentStatus.PENDING;
    @Column(length = 2000) private String rejectionReason;
    private Instant verifiedAt;
}
