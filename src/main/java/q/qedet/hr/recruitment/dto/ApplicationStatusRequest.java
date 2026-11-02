package q.qedet.hr.recruitment.dto;

import jakarta.validation.constraints.NotNull;
import q.qedet.hr.recruitment.model.ApplicationStatus;

public record ApplicationStatusRequest(@NotNull ApplicationStatus status) {
}
