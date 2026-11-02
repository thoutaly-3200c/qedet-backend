package q.qedet.organization.policy.dto;
import q.qedet.organization.policy.entity.WorkdayDefinition; import q.qedet.organization.policy.model.WorkScheduleStatus; import java.time.Instant; import java.util.*;
public record WorkScheduleResponse(UUID id, UUID organizationId, String name, String description, String timezone, WorkScheduleStatus status, Integer gracePeriodMinutes, boolean organizationDefault, List<WorkdayDefinition> workdays, Instant createdAt, Instant updatedAt) {}
