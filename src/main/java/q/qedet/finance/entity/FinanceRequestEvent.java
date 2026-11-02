package q.qedet.finance.entity;
import jakarta.persistence.*; import lombok.*; import q.qedet.common.model.TimestampedEntity; import q.qedet.finance.model.FinanceWorkflowTypes.RequestStatus; import java.time.Instant; import java.util.UUID;
@Entity @Table(name = "finance_request_events", indexes = @Index(name = "idx_finance_request_event_request_created", columnList = "finance_request_id,created_at"))
@Getter @Setter @NoArgsConstructor public class FinanceRequestEvent extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "finance_request_id", nullable = false) private FinanceRequest request;
    @Enumerated(EnumType.STRING) @Column(length = 20) private RequestStatus priorStatus;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RequestStatus newStatus;
    @Column(nullable = false, length = 60) private String action; @Column(nullable = false) private UUID actorId; @Column(length = 2000) private String notes;
    @Column(nullable = false) private Instant occurredAt;
}
