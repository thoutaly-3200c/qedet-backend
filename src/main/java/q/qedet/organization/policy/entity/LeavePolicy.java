package q.qedet.organization.policy.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.Organization;
import java.time.LocalDate;

@Entity @Table(name="hr_leave_policies", indexes=@Index(name="idx_leave_policy_effective",columnList="organization_id,effective_from"))
@Getter @Setter @NoArgsConstructor
public class LeavePolicy extends TimestampedEntity {
 @ManyToOne(optional=false) @JoinColumn(name="organization_id",nullable=false) private Organization organization;
 @Column(nullable=false) private LocalDate effectiveFrom;
 @Column(nullable=false) private boolean enabled=true, allowEmployeeRequests=true, requireApproval=true, reasonRequired, attachmentRequired, allowHalfDayLeave=true, allowHourlyLeave, allowNegativeBalance, carryOverEnabled;
 @Column(nullable=false) private Integer minimumNoticeDays=0;
 @Column(nullable=false) private java.math.BigDecimal carryOverMaximumDays=java.math.BigDecimal.ZERO;
}
