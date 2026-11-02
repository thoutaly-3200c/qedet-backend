package q.qedet.hr.payroll.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.Organization;
import java.time.LocalDate;

@Entity @Table(name = "hr_payroll_periods") @Getter @Setter @NoArgsConstructor
public class PayrollPeriod extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate endDate;
    @Column(nullable = false) private boolean open;
    @Column(nullable = false) private boolean finalized;
}
