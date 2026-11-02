    package q.qedet.procurement.entity;

    import jakarta.persistence.*; import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
    import q.qedet.common.model.TimestampedEntity; import q.qedet.organization.entity.Organization;
    import java.math.BigDecimal; import java.time.LocalDate; import java.util.ArrayList; import java.util.List;

    @Entity
    @Table(name = "purchase_requests",
            uniqueConstraints = @UniqueConstraint(name = "uk_purchase_request_org_number",
                    columnNames = {"organization_id", "number"}))
    @Getter @Setter @NoArgsConstructor
    public class PurchaseRequest extends TimestampedEntity {
        @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
        @Column(nullable = false, length = 64) private String number;
        @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private RequestType requestType;
        @Column(nullable = false, length = 255) private String title;
        @Column(nullable = false, length = 2000) private String reason;
        @Column(name = "requested_by_id", nullable = false) private java.util.UUID requestedBy;
        @Column(length = 160) private String department;
        @Column(length = 60) private String sourceDomain;
        @Column(length = 160) private String sourceReference;
        @Column(nullable = false) private LocalDate date;
        private LocalDate neededBy;
        @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Priority priority = Priority.NORMAL;
        @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
        private ProcurementStatus procurementStatus = ProcurementStatus.SUBMITTED;
        @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
        private OriginatingRequestStatus originatingRequestStatus = OriginatingRequestStatus.OPEN;
        @Column(length = 2000) private String notes;
        @Column(length = 1000) private String attachmentReference;
        @Column(precision = 14, scale = 2, nullable = false) private BigDecimal totalEstimate = BigDecimal.ZERO;
        @Column(length = 3) private String currency;
        @Version private Long version;
        @OneToMany(mappedBy = "purchaseRequest", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<PurchaseRequestItem> items = new ArrayList<>();

        public enum RequestType { STANDARD, REPLENISHMENT }
        public enum Priority { LOW, NORMAL, HIGH }
        public enum ProcurementStatus { SUBMITTED, APPROVED, REJECTED, COMPLETED }
        public enum OriginatingRequestStatus { OPEN, CANCELLED }
    }
