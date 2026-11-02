package q.qedet.organization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.DepartmentStatus;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.policy.entity.WorkSchedule;


@Entity
@Table(name = "departments", uniqueConstraints = @UniqueConstraint(
        name = "uk_departments_organization_name", columnNames = {"organization_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
public class Department extends TimestampedEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne
    @JoinColumn(name = "parent_department_id")
    private Department parentDepartment;

    @ManyToOne
    @JoinColumn(name = "schedule_id")
    private WorkSchedule schedule;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DepartmentStatus status = DepartmentStatus.ACTIVE;

    public Department(Organization organization, String name, String code) {
        this.organization = organization;
        this.name = name;
        this.code = code;
    }
}
