package q.qedet.finance.entity;
import jakarta.persistence.*; import lombok.*; import q.qedet.common.model.TimestampedEntity; import q.qedet.finance.model.FinanceWorkflowTypes.ConfirmationStatus; import q.qedet.organization.entity.Organization; import java.time.Instant; import java.util.UUID;
@Entity @Table(name = "finance_transfer_confirmations", uniqueConstraints = @UniqueConstraint(name = "uk_finance_transfer_confirmation_operation", columnNames = "operation_id"), indexes = @Index(name = "idx_finance_transfer_confirmation_token", columnList = "qr_token"))
@Getter @Setter @NoArgsConstructor public class FinanceTransferConfirmation extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization; @Column(name = "operation_id", nullable = false) private UUID operationId;
    private UUID senderId; private UUID receiverId; @Column(name = "qr_token", nullable = false, length = 128) private String qrToken; @Column(length = 160) private String qrReference;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ConfirmationStatus status = ConfirmationStatus.PENDING; private Instant verifiedAt; private Instant confirmedAt; private Instant expiresAt;
}
