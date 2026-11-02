package q.qedet.organization.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;

import java.math.BigDecimal;

/** Optional storage position below a zone. Ownership is derived from the zone. */
@Entity
@Table(name = "site_bins", uniqueConstraints = @UniqueConstraint(name = "uk_site_bin_code", columnNames = {"zone_id", "code"}),
        indexes = @Index(name = "idx_site_bin_zone_active", columnList = "zone_id,active"))
@Getter @Setter @NoArgsConstructor
public class OrganizationBin extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "zone_id", nullable = false)
    private OrganizationZone zone;
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 160) private String name;
    @Column(length = 500) private String description;
    @Column(nullable = false) private boolean active = true;
    @Column(precision = 14, scale = 2) private BigDecimal capacity;
    @Version private Long version;

    public OrganizationBin(OrganizationZone zone, String code, String name) {
        this.zone = zone;
        this.code = code;
        this.name = name;
    }
}
