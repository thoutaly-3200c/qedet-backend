package q.qedet.organization.policy.dto;
import java.time.*; import java.util.*; import q.qedet.organization.policy.model.AssignmentStatus;
public record DepartmentScheduleAssignmentResponse(UUID id, UUID departmentId, UUID scheduleId,
                                                   LocalDate effectiveFrom, LocalDate effectiveTo,
                                                   AssignmentStatus status) {}
