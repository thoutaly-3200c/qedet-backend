package q.qedet.hr.attendance.dto;

import jakarta.validation.constraints.NotNull;
import q.qedet.hr.attendance.model.PunchSource;
import java.time.Instant;

public record PunchRequest(@NotNull PunchSource source, Instant punchedAt, String note) {}
