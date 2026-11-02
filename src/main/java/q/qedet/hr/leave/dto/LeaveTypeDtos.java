package q.qedet.hr.leave.dto;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
public final class LeaveTypeDtos {
 private LeaveTypeDtos(){}
 public record Request(@NotBlank String name,@NotBlank @Pattern(regexp="[A-Za-z0-9_-]+") String code,String description,boolean paid,boolean requiresApproval,boolean requiresAttachment,@Min(0) Integer minimumNoticeDays,@Min(1) Integer maximumConsecutiveDays,@NotNull @DecimalMin("0") BigDecimal annualAllowance){}
 public record Response(UUID id,String name,String code,String description,boolean paid,boolean requiresApproval,boolean requiresAttachment,Integer minimumNoticeDays,Integer maximumConsecutiveDays,BigDecimal annualAllowance,boolean active,Instant createdAt,Instant updatedAt){}
}
