package q.qedet.organization.policy.dto;
import jakarta.validation.constraints.*; import q.qedet.organization.policy.model.HolidayType; import java.time.*; import java.util.*;
public final class OrganizationHolidayDtos {
 private OrganizationHolidayDtos(){}
 public record Request(@NotBlank String name,@NotNull LocalDate date,boolean recurring,@NotNull HolidayType type,Boolean active){}
 public record Response(UUID id,String name,LocalDate date,boolean recurring,HolidayType type,boolean active,Instant createdAt,Instant updatedAt){}
}
