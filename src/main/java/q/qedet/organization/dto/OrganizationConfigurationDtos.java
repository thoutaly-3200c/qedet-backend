package q.qedet.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import q.qedet.organization.entity.OrganizationModuleType;
import q.qedet.organization.entity.SequenceType;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

public final class OrganizationConfigurationDtos {
    private OrganizationConfigurationDtos() {}
    public record LocalizationRequest(@NotBlank String country, @NotBlank String timezone, @NotBlank String currency,
                                      @NotBlank String language, @NotBlank String dateFormat, @NotBlank String timeFormat,
                                      @NotNull DayOfWeek firstDayOfWeek) {}
    public record CommunicationRequest(Boolean emailEnabled, Boolean smsEnabled, Boolean pushNotificationsEnabled,
                                       String defaultSenderName, @Email String defaultSenderEmail) {}
    public record SecurityRequest(@Min(1) @Max(10080) Integer sessionTimeoutMinutes, Boolean mfaEnabled,
                                  Boolean mfaRequired, Boolean allowMultipleSessions) {}
    public record ModuleRequest(@NotNull OrganizationModuleType module, boolean enabled) {}
    public record ModulesRequest(@NotNull List<ModuleRequest> modules) {}
    public record NumberingRequest(String prefix, @Min(1) Long nextNumber, @Min(1) @Max(32) Integer numberLength) {}
    public record SalesTaxRequest(@NotNull @jakarta.validation.constraints.DecimalMin("0.0000") @jakarta.validation.constraints.DecimalMax("100.0000") BigDecimal ratePercent,
                                  boolean pricesTaxInclusive) {}
    public record SalesTaxResponse(BigDecimal ratePercent, boolean pricesTaxInclusive) {}
    public record NumberingResponse(UUID id, SequenceType sequenceType, String prefix, Long nextNumber, Integer numberLength,
                                    Instant createdAt, Instant updatedAt) {}
    public record ConfigurationResponse(String country, String timezone, String currency, String language, String dateFormat,
                                        String timeFormat, DayOfWeek firstDayOfWeek, boolean emailEnabled, boolean smsEnabled,
                                        boolean pushNotificationsEnabled, String defaultSenderName, String defaultSenderEmail,
                                        Integer sessionTimeoutMinutes, boolean mfaEnabled, boolean mfaRequired,
                                        boolean allowMultipleSessions) {}
}
