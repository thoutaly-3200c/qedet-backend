package q.qedet.organization.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.RoleType;
import q.qedet.common.model.TimestampedEntity;

@Entity
@Table(name = "positions",
        uniqueConstraints = @UniqueConstraint(name = "uk_positions_organization_code", columnNames = {"organization_id", "code"}),
        indexes = {
                @Index(name = "idx_positions_organization", columnList = "organization_id"),
                @Index(name = "idx_positions_department", columnList = "department_id"),
                @Index(name = "idx_positions_default_site", columnList = "default_site_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class Position extends TimestampedEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne
    @JoinColumn(name = "default_site_id")
    private OrganizationSite defaultSite;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private RoleType defaultRole;

    @Column(nullable = false)
    private boolean systemPosition;

    public Position(Organization organization, Department department, OrganizationSite defaultSite,
                    String name, String code, RoleType defaultRole, boolean systemPosition) {
        this.organization = organization;
        this.department = department;
        this.defaultSite = defaultSite;
        this.name = name;
        this.code = code;
        this.defaultRole = defaultRole;
        this.systemPosition = systemPosition;
    }
}
