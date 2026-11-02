package q.qedet.organization.policy.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.policy.model.*;
import q.qedet.organization.entity.Organization;
import java.time.LocalDate;

@Entity @Table(name="hr_attendance_policies", indexes=@Index(name="idx_attendance_policy_effective",columnList="organization_id,effective_from"))
@Getter @Setter @NoArgsConstructor
public class AttendancePolicy extends TimestampedEntity {
 @ManyToOne(optional=false) @JoinColumn(name="organization_id",nullable=false) private Organization organization;
 @Column(nullable=false) private LocalDate effectiveFrom;
 @Column(nullable=false) private boolean enabled=true, requireCheckIn=true, requireCheckOut=true;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private CheckInMethod checkInMethod=CheckInMethod.QR;
 @Column(nullable=false) private Integer gracePeriodMinutes=0; @Column(nullable=false) private boolean allowEarlyCheckIn;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private CheckOutMethod checkOutMethod=CheckOutMethod.QR;
 @Column(nullable=false) private boolean allowEarlyCheckout, earlyCheckoutRequiresApproval;
 @Column(nullable=false) private boolean requireAttendanceQr=true, requireLocation=true, allowRemoteAttendance;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private MissingCheckoutHandling missingCheckoutHandling=MissingCheckoutHandling.REQUIRES_REVIEW;
 @Column(nullable=false) private boolean allowEmployeeCorrectionRequest, correctionRequiresApproval=true, correctionReasonRequired=true;
}
