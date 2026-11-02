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

@Entity @Table(name = "sales_stock_requests") @Getter @Setter @NoArgsConstructor
public class SalesStockRequest extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id") private Organization organization;
    @Column(nullable = false) private String status = "DRAFT";
    @Column(length = 2000) private String notes;
    @Column(nullable = false) private UUID createdBy;
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true) private List<SalesStockRequestItem> items = new ArrayList<>();
}
