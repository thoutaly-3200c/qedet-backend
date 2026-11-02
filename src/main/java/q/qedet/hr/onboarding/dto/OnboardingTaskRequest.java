package q.qedet.hr.onboarding.dto;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
public record OnboardingTaskRequest(@NotBlank String title, String description, UUID assigneeEmployeeId) {}
