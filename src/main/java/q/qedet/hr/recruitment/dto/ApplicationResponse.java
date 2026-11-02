package q.qedet.hr.recruitment.dto;

import q.qedet.hr.recruitment.entity.Application;
import q.qedet.hr.recruitment.model.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        UUID jobOpeningId,
        UUID organizationId,
        UUID candidateId,
        ApplicationStatus status,
        String resumeKey,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static ApplicationResponse from(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getJobOpening().getId(),
                application.getOrganization().getId(),
                application.getCandidate().getId(),
                application.getStatus(),
                application.getResumeKey(),
                application.getNotes(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}
