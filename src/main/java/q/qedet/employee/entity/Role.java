package q.qedet.employee.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.Organization;

@Entity
@Table(name = "roles", uniqueConstraints = @UniqueConstraint(
        name = "uk_roles_organization_name", columnNames = {"organization_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
public class Role extends TimestampedEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String code;

    private String description;

    @Column(nullable = false)
    private boolean systemRole;
}
