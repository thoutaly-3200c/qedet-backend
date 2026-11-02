package q.qedet.hr.recruitment.dto;

import q.qedet.hr.recruitment.entity.JobOpening;
import q.qedet.hr.recruitment.model.EmploymentType;
import q.qedet.hr.recruitment.model.JobOpeningStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

public record JobOpeningResponse(
        UUID id,
        UUID organizationId,
        String title,
        UUID departmentId,
        UUID positionId,
        UUID locationId,
        EmploymentType employmentType,
        String description,
        List<String> requirements,
        List<String> responsibilities,
        Integer vacancies,
        LocalDate applicationDeadline,
        JobOpeningStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static JobOpeningResponse from(JobOpening job) {
        return new JobOpeningResponse(
                job.getId(),
                job.getOrganization().getId(),
                job.getTitle(),
                job.getDepartmentId(),
                job.getPositionId(),
                job.getLocationId(),
                job.getEmploymentType(),
                job.getDescription(),
                job.getRequirements() == null ? List.of() : new ArrayList<>(job.getRequirements()),
                job.getResponsibilities() == null ? List.of() : new ArrayList<>(job.getResponsibilities()),
                job.getVacancies(),
                job.getApplicationDeadline(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
