package q.qedet.finance.entity;
import jakarta.persistence.*; import lombok.*; import q.qedet.common.model.TimestampedEntity; import java.util.UUID;
@Entity @Table(name = "finance_request_attachments", indexes = @Index(name = "idx_finance_request_attachment_request", columnList = "finance_request_id"))
@Getter @Setter @NoArgsConstructor public class FinanceRequestAttachment extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "finance_request_id", nullable = false) private FinanceRequest request;
    @Column(nullable = false, length = 1000) private String storageReference; @Column(nullable = false, length = 255) private String filename;
    @Column(nullable = false, length = 127) private String contentType; @Column(nullable = false) private UUID uploadedBy;
}
