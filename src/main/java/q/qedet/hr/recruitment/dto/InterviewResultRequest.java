package q.qedet.hr.recruitment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import q.qedet.hr.recruitment.model.InterviewResult;

public record InterviewResultRequest(
        @NotNull InterviewResult result,
        @Size(max = 4000) String feedback
) {
}
