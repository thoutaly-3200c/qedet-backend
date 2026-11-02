package q.qedet.employee.dto;

import java.time.LocalDate;

public record TerminationRequest(
        String reason,
        LocalDate effectiveDate,
        LocalDate finalPayDate,
        String notes
) {
}
