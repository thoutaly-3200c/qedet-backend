package q.qedet.qr.dto;

import jakarta.validation.constraints.NotNull;
import q.qedet.qr.entity.QrCodeType;
import q.qedet.qr.entity.QrTargetKind;

import java.util.UUID;
import java.time.Instant;

public record QrCodeRequest(
        @NotNull QrCodeType type,
        @NotNull UUID targetId,
        QrTargetKind targetKind,
        Instant expiresAt
) {
    public QrCodeRequest(QrCodeType type, UUID targetId, QrTargetKind targetKind) {
        this(type, targetId, targetKind, null);
    }
}
