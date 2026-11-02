package q.qedet.organization.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;

import java.math.BigDecimal;

/** An operational subdivision of a physical organization site. */
@Entity
@Table(name = "site_zones", uniqueConstraints = @UniqueConstraint(name = "uk_site_zone_code", columnNames = {"organization_site_id", "code"}),
        indexes = @Index(name = "idx_site_zone_site_active", columnList = "organization_site_id,active"))
@Getter @Setter @NoArgsConstructor
public class OrganizationZone extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_site_id", nullable = false)
    private OrganizationSite site;
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 160) private String name;
    @Column(length = 500) private String description;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(precision = 14, scale = 2) private BigDecimal capacity;
    @Version private Long version;

    public OrganizationZone(OrganizationSite site, String code, String name) {
        this.site = site;
        this.code = code;
        this.name = name;
    }
}
