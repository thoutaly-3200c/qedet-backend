package q.qedet.hr.recruitment.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import q.qedet.hr.recruitment.model.EmploymentType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateJobRequest(
        @NotBlank String title,
        @NotNull UUID departmentId,
        @NotNull UUID positionId,
        @NotNull UUID locationId,
        @NotNull EmploymentType employmentType,
        String description,
        List<String> requirements,
        List<String> responsibilities,
        @NotNull @Positive Integer vacancies,
        @NotNull @FutureOrPresent LocalDate applicationDeadline
) {
}
