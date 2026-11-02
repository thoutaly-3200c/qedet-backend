package q.qedet.organization.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.LocationStatus;
import q.qedet.common.model.LocationType;
import q.qedet.common.model.TimestampedEntity;

/** A physical place owned by an organization. Address fields describe the site; they do not identify it. */
@Entity
@Table(name = "organization_sites", uniqueConstraints = {
        @UniqueConstraint(name = "uk_organization_site_org_code", columnNames = {"organization_id", "code"})
}, indexes = @Index(name = "idx_organization_site_org_status", columnList = "organization_id,status"))
@Getter @Setter @NoArgsConstructor
public class OrganizationSite extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @Column(nullable = false, length = 40) private String code;
    @Column(nullable = false, length = 160) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private LocationType type;
    @Column(length = 500) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private LocationStatus status = LocationStatus.ACTIVE;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(length = 240) private String addressLine1;
    @Column(length = 240) private String addressLine2;
    @Column(length = 100) private String city;
    @Column(length = 100) private String state;
    @Column(length = 30) private String postalCode;
    @Column(length = 100) private String country;
    @Column(length = 40) private String phone;
    @Column(length = 254) private String email;
    @Version private Long version;

    public OrganizationSite(Organization organization, String code, String name, LocationType type) {
        this.organization = organization;
        this.code = code;
        this.name = name;
        this.type = type;
    }
}
