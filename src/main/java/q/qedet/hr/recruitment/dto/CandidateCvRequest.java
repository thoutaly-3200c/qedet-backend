package q.qedet.hr.recruitment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CandidateCvRequest(
        @NotBlank @Size(max = 255) String fileName,
        @NotBlank @Size(max = 2000) String storageKey,
        @Size(max = 100) String contentType
) {
}
