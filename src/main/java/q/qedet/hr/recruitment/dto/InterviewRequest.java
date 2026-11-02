package q.qedet.hr.recruitment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record InterviewRequest(
        @NotNull UUID interviewerId,
        @NotNull Instant scheduledAt,
        @Size(max = 4000) String notes,
        @Size(max = 2000) String meetingLink,
        @jakarta.validation.constraints.Min(1) Integer durationMinutes,
        @Size(max = 500) String location
) {
    public InterviewRequest(Instant scheduledAt, Integer durationMinutes, String location,
                            String meetingUrl, String notes) {
        this(null, scheduledAt, notes, meetingUrl, durationMinutes, location);
    }
}
