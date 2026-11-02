package q.qedet.hr.recruitment.dto;

import q.qedet.hr.recruitment.entity.CandidateCv;

import java.time.Instant;
import java.util.UUID;

public record CandidateCvResponse(
        UUID id,
        UUID candidateId,
        String fileName,
        String storageKey,
        String contentType,
        Instant createdAt,
        Instant updatedAt
) {
    public static CandidateCvResponse from(CandidateCv cv) {
        return new CandidateCvResponse(
                cv.getId(),
                cv.getCandidate().getId(),
                cv.getFileName(),
                cv.getStorageKey(),
                cv.getContentType(),
                cv.getCreatedAt(),
                cv.getUpdatedAt()
        );
    }
}
