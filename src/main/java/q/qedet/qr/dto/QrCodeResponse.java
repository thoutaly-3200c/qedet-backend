package q.qedet.qr.dto;

import q.qedet.qr.entity.QrCodeStatus;
import q.qedet.qr.entity.QrCodeType;
import q.qedet.qr.entity.QrTargetKind;

import java.time.Instant;
import java.util.UUID;

public record QrCodeResponse(
        UUID id,
        String code,
        QrCodeType type,
        QrTargetKind targetKind,
        UUID organizationId,
        UUID referenceId,
        QrCodeStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt,
        QrProductResponse product
) {
    public QrCodeResponse(UUID id, String code, QrCodeType type, QrTargetKind targetKind,
                          UUID organizationId, UUID referenceId, QrCodeStatus status,
                          Instant createdAt, Instant updatedAt, Instant expiresAt) {
        this(id, code, type, targetKind, organizationId, referenceId, status,
                createdAt, updatedAt, expiresAt, null);
    }

    public String publicIdentifier() { return code; }
    public UUID targetId() { return referenceId; }
}
