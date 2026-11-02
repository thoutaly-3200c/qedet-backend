package q.qedet.hr.leave.entity;

import jakarta.persistence.*;
import lombok.*;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.organization.entity.Organization;
import java.math.BigDecimal;

@Entity @Table(name="hr_leave_balances", uniqueConstraints=@UniqueConstraint(name="uk_leave_balance_employee_type_period",columnNames={"employee_id","leave_type_id","balance_year"}))
@Getter @Setter @NoArgsConstructor
public class LeaveBalance extends TimestampedEntity {
 @ManyToOne(optional=false) @JoinColumn(name="organization_id",nullable=false) private Organization organization;
 @ManyToOne(optional=false) @JoinColumn(name="employee_id",nullable=false) private Employee employee;
 @ManyToOne(optional=false) @JoinColumn(name="leave_type_id",nullable=false) private LeaveType leaveType;
 @Column(name="balance_year",nullable=false) private Integer year;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal allowance=BigDecimal.ZERO, carriedOver=BigDecimal.ZERO, used=BigDecimal.ZERO, pending=BigDecimal.ZERO;
 public BigDecimal getRemaining(){return allowance.add(carriedOver).subtract(used).subtract(pending);}
}
