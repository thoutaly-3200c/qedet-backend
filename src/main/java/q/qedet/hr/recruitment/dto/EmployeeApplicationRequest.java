package q.qedet.hr.recruitment.dto;

import jakarta.validation.constraints.Size;

public record EmployeeApplicationRequest(
        @Size(max = 2000) String resumeKey,
        @Size(max = 2000) String notes
) {
}
