package q.qedet.employee.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.auth.entity.UserAccount;
import q.qedet.common.model.EmployeeStatus;
import q.qedet.common.model.RoleType;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.Department;
import q.qedet.organization.entity.Organization;
import q.qedet.organization.entity.OrganizationSite;
import q.qedet.organization.entity.Position;

@Entity
@Table(name = "employees", uniqueConstraints = @UniqueConstraint(
        name = "uk_employees_organization_number", columnNames = {"organization_id", "employee_number"}))
@Getter
@Setter
@NoArgsConstructor
public class Employee extends TimestampedEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(nullable = false)
    private String employeeNumber;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column
    private String email;

    @Column
    private String phone;

    @Column
    private Long salary;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private OrganizationSite site;

    @ManyToOne
    @JoinColumn(name = "position_id")
    private Position position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleType role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeStatus status = EmployeeStatus.CREATED;

    @Column(name = "hire_date")
    private java.time.Instant joinedAt;

    @Column
    private java.time.Instant invitedAt;

    @Column
    private java.time.Instant activatedAt;

    @Column
    private java.time.Instant deactivatedAt;

    @Column
    private java.time.Instant terminatedAt;

    public Employee(Organization organization, String employeeNumber, String firstName, String lastName, String email, String phone, RoleType role) {
        this.organization = organization;
        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.role = role;
    }
}
