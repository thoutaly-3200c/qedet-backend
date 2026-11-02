package q.qedet.organization.policy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import q.qedet.organization.policy.entity.WorkdayDefinition;

import java.util.List;

public record WorkScheduleRequest(
        @NotBlank String name,
        String description,
        String timezone,
        @Min(0) Integer gracePeriodMinutes,
        Boolean organizationDefault,
        List<WorkdayDefinition> workdays
) {
}
