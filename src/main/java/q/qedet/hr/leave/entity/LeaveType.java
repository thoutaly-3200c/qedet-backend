package q.qedet.hr.leave.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.Organization;
import java.math.BigDecimal;

@Entity @Table(name = "hr_leave_types", uniqueConstraints = @UniqueConstraint(name = "uk_leave_type_org_code", columnNames = {"organization_id", "code"})) @Getter @Setter @NoArgsConstructor
public class LeaveType extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @Column(nullable = false) private String code;
    @Column(nullable = false) private String name;
    @Column(length = 2000) private String description;
    @Column(nullable = false) private boolean paid;
    @Column(nullable = false) private boolean requiresApproval = true;
    @Column(nullable = false) private boolean requiresAttachment;
    @Column(nullable = false) private Integer minimumNoticeDays = 0;
    @Column private Integer maximumConsecutiveDays;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal annualAllowance = BigDecimal.ZERO;
    @Column(nullable = false) private boolean active = true;
}
