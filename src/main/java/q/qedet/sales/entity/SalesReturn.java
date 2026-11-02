package q.qedet.sales.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.Organization;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity @Table(name = "sales_returns") @Getter @Setter @NoArgsConstructor
public class SalesReturn extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id") private Organization organization;
    @Column(nullable = false) private UUID salesHallId;
    @Column(nullable = false) private UUID destinationWarehouseId;
    @Column(nullable = false) private String status = "DRAFT";
    @Column(length = 1000) private String reason;
    @Column(length = 2000) private String notes;
    @Column(nullable = false) private UUID createdBy;
    @OneToMany(mappedBy = "salesReturn", cascade = CascadeType.ALL, orphanRemoval = true) private List<SalesReturnItem> items = new ArrayList<>();
}
