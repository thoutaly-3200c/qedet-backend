package q.qedet.finance.entity;
import jakarta.persistence.*; import lombok.*; import q.qedet.common.model.TimestampedEntity; import java.util.UUID;
@Entity @Table(name = "finance_operation_attachments", indexes = @Index(name = "idx_finance_operation_attachment_operation", columnList = "operation_id"))
@Getter @Setter @NoArgsConstructor public class FinanceOperationAttachment extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "operation_id", nullable = false) private FinanceOperation operation;
    @Column(nullable = false, length = 1000) private String storageReference; @Column(nullable = false, length = 255) private String filename; @Column(nullable = false, length = 127) private String contentType; @Column(nullable = false) private UUID uploadedBy;
}
