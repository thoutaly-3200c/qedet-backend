package q.qedet.qr.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.organization.entity.Organization;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "qr_codes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_qr_codes_organization_code",
                columnNames = {"organization_id", "code"})
}, indexes = {
        @Index(name = "idx_qr_codes_code", columnList = "code"),
        @Index(name = "idx_qr_codes_organization", columnList = "organization_id"),
        @Index(name = "idx_qr_codes_target_identity",
                columnList = "organization_id,type,target_kind,reference_id")
})
@Getter
@Setter
@NoArgsConstructor
public class QrCode extends TimestampedEntity {
    @Column(name = "code", nullable = false, length = 255)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private QrCodeType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_kind", length = 32)
    private QrTargetKind targetKind;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QrCodeStatus status = QrCodeStatus.ACTIVE;

    @Column
    private Instant expiresAt;

    public String getPublicIdentifier() { return code; }
    public void setPublicIdentifier(String value) { this.code = value; }
    public UUID getTargetId() { return referenceId; }
    public void setTargetId(UUID value) { this.referenceId = value; }
    public boolean isExpired(Instant now) { return expiresAt != null && !expiresAt.isAfter(now); }
}
