package q.qedet.organization.dto;
import jakarta.validation.constraints.*; import java.math.BigDecimal;
public record OrganizationBinRequest(@NotBlank @Size(max=64) String code, @NotBlank @Size(max=160) String name,
                                     @Size(max=500) String description, Boolean active, BigDecimal capacity) { }
