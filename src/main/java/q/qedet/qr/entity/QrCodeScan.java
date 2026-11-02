package q.qedet.qr.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "qr_code_scans", indexes = {
        @Index(name = "idx_qr_scans_qr_code", columnList = "qr_code_id"),
        @Index(name = "idx_qr_scans_scanned_at", columnList = "scanned_at")
})
@Getter
@Setter
@NoArgsConstructor
public class QrCodeScan extends TimestampedEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qr_code_id")
    private QrCode qrCode;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "scanned_at", nullable = false)
    private Instant scannedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QrScanResult result;

    @Column(length = 64)
    private String ipAddress;

    @Column(length = 512)
    private String userAgent;

    public QrCodeScan(QrCode qrCode, UUID userId, QrScanResult result) {
        this.qrCode = qrCode;
        this.userId = userId;
        this.organizationId = qrCode == null || qrCode.getOrganization() == null
                ? null : qrCode.getOrganization().getId();
        this.scannedAt = Instant.now();
        this.result = result;
    }
}
