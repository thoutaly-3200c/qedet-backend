package q.qedet.hr.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.hr.performance.model.PerformanceReviewStatus;
import q.qedet.organization.entity.Organization;
import java.math.BigDecimal;

@Entity @Table(name = "hr_performance_reviews") @Getter @Setter @NoArgsConstructor
public class PerformanceReview extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @ManyToOne(optional = false) @JoinColumn(name = "employee_id", nullable = false) private Employee employee;
    @Column(nullable = false) private String period;
    @Column(length = 4000) private String summary;
    @Column(precision = 5, scale = 2) private BigDecimal rating;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PerformanceReviewStatus status = PerformanceReviewStatus.DRAFT;
}
