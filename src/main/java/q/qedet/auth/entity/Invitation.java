package q.qedet.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import q.qedet.common.model.InvitationStatus;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.employee.entity.Employee;
import q.qedet.organization.entity.Organization;

import java.time.Instant;

@Entity
@Table(name = "invitations", uniqueConstraints = @jakarta.persistence.UniqueConstraint(
        name = "uk_invitations_organization_email_status",
        columnNames = {"organization_id", "target_email", "status"}))
@Getter
@Setter
@NoArgsConstructor
public class Invitation extends TimestampedEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(nullable = false)
    private String targetEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column
    private Instant usedAt;

    public Invitation(Organization organization, Employee employee, String tokenHash, String targetEmail, Instant expiresAt) {
        this.organization = organization;
        this.employee = employee;
        this.tokenHash = tokenHash;
        this.targetEmail = targetEmail;
        this.expiresAt = expiresAt;
    }
}
