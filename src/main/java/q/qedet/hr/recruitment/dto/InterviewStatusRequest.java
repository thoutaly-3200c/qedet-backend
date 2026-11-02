package q.qedet.hr.recruitment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import q.qedet.hr.recruitment.model.InterviewStatus;

public record InterviewStatusRequest(
        @NotNull InterviewStatus status,
        @Size(max = 4000) String feedback
) {
}
