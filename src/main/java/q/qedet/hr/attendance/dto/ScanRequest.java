package q.qedet.hr.attendance.dto;
import jakarta.validation.constraints.NotBlank;
public record ScanRequest(@NotBlank String qrToken) {}
