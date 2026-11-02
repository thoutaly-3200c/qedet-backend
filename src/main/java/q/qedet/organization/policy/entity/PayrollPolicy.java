package q.qedet.organization.policy.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.policy.model.*;
import q.qedet.organization.entity.Organization;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name="hr_payroll_policies", indexes=@Index(name="idx_payroll_policy_effective",columnList="organization_id,effective_from"))
@Getter @Setter @NoArgsConstructor
public class PayrollPolicy extends TimestampedEntity {
 @ManyToOne(optional=false) @JoinColumn(name="organization_id",nullable=false) private Organization organization;
 @Column(nullable=false) private LocalDate effectiveFrom;
 @Column(nullable=false) private boolean enabled=true, payrollApprovalRequired, overtimeAuthorizationRequired=true, lateDeductionEnabled, absenceDeductionEnabled, unpaidLeaveDeductionEnabled;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private PayrollFrequency payrollFrequency=PayrollFrequency.MONTHLY;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private PayDateRule payDateRule=PayDateRule.LAST_WORKING_DAY;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private OvertimeCalculationMethod overtimeCalculationMethod=OvertimeCalculationMethod.APPROVED_MINUTES;
 @Column(nullable=false,precision=8,scale=3) private BigDecimal weekdayOvertimeMultiplier=BigDecimal.ONE;
 @Column(nullable=false,precision=8,scale=3) private BigDecimal weekendOvertimeMultiplier=BigDecimal.ONE;
 @Column(nullable=false,precision=8,scale=3) private BigDecimal holidayOvertimeMultiplier=BigDecimal.ONE;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private MissingAttendanceHandling missingAttendanceHandling=MissingAttendanceHandling.REQUIRES_REVIEW;
 @Column(nullable=false) private Integer payrollCutoffDay=1;
}
