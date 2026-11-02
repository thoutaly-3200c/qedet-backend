package q.qedet.procurement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.Organization;

@Entity
@Table(name = "delivery_locations", indexes = {
        @Index(name = "idx_delivery_location_org_name", columnList = "organization_id,name")
})
@Getter
@Setter
@NoArgsConstructor
public class DeliveryLocation extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @Column(nullable = false) private String name;
    private String address;
    private String city;
    private String country;
    private String contact;
    private String phone;
    private boolean active = true;
}
