package q.qedet.warehouse.entity;
import jakarta.persistence.*; import lombok.*; import q.qedet.common.model.TimestampedEntity; import q.qedet.organization.entity.Organization; import q.qedet.organization.entity.OrganizationZone; import java.math.BigDecimal;
@Entity @Table(name="bin_locations") @Getter @Setter @NoArgsConstructor public class BinLocation extends TimestampedEntity {
 @ManyToOne(optional=false) @JoinColumn(name="organization_id") private Organization organization; @ManyToOne(optional=false) @JoinColumn(name="zone_id") private OrganizationZone zone;
 @Column(nullable=false) private String code; private String name; private BigDecimal capacity; private boolean active=true;
}
