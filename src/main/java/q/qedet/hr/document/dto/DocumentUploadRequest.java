package q.qedet.hr.document.dto;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
public record DocumentUploadRequest(@NotBlank String documentType, @NotBlank String fileName,
                                    @NotBlank String storageKey, LocalDate expiresOn) {}
