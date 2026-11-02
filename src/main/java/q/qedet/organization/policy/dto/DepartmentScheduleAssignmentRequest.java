package q.qedet.organization.policy.dto;
import jakarta.validation.constraints.*; import java.time.LocalDate; import java.util.*;
public record DepartmentScheduleAssignmentRequest(@NotNull UUID departmentId, @NotNull UUID scheduleId,
                                                  @NotNull LocalDate effectiveFrom,
                                                  LocalDate effectiveTo) {}
