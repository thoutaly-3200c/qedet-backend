package q.qedet.organization.policy.dto;
import java.time.*; import java.util.*; import q.qedet.organization.policy.model.AssignmentStatus;
public record ScheduleAssignmentResponse(UUID id, UUID employeeId, UUID scheduleId, LocalDate effectiveFrom, LocalDate effectiveTo, AssignmentStatus status) {}
