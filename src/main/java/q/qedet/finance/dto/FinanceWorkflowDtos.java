package q.qedet.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class FinanceWorkflowDtos {
    private FinanceWorkflowDtos() {}

    public record Attachment(@NotBlank @Size(max = 1000) String storageReference,
                             @NotBlank @Size(max = 255) String filename,
                             @NotBlank @Size(max = 127) String contentType) {}
    public record CreateRequest(@NotBlank @Size(max = 60) String sourceDomain,
                                @NotBlank @Size(max = 160) String sourceRecordId,
                                @NotBlank @Size(max = 60) String type,
                                @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
                                @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
                                @NotBlank @Size(max = 2000) String reason,
                                @Size(max = 4000) String notes,
                                @Size(max = 500) String supportingReference,
                                LocalDate dueDate, List<Attachment> attachments) {}
    public record RequestDetails(UUID supplierId, UUID purchaseOrderId, UUID employeeId, UUID payrollId,
                                UUID departmentId, UUID cashBoxId, UUID recipientEmployeeId,
                                UUID originalOperationId, String payeeName, String purpose,
                                String invoiceReference, String expenseCategory, String varianceReason,
                                BigDecimal expectedAmount, BigDecimal countedAmount, LocalDate settlementDate) {}
    public record CreateFinancialRequest(@NotBlank String type, @NotNull @DecimalMin(value = "0.01") BigDecimal requestedAmount,
                                        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
                                        @NotBlank @Size(max = 2000) String reason, @Size(max = 4000) String notes,
                                        LocalDate dueDate, String sourceType, String sourceId,
                                        @NotNull RequestDetails details, List<Attachment> attachments) {}
    public record ApproveRequest(@NotNull @DecimalMin(value = "0.01") BigDecimal approvedAmount, @Size(max = 2000) String notes) {}
    public record Review(@Size(max = 2000) String notes) {}
    public record CreateAccount(@NotBlank @Size(max = 20) String type, @NotBlank @Size(max = 160) String name,
                                UUID locationId, @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
                                UUID assignedEmployeeId) {}
    public record CreateOperation(@NotBlank @Size(max = 30) String type, UUID sourceAccountId,
                                  UUID destinationAccountId, @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
                                  @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
                                  @Size(max = 160) String reference, @Size(max = 2000) String description,
                                  @Size(max = 4000) String notes, UUID requestId,
                                  @NotBlank @Size(max = 128) String idempotencyKey,
                                  List<Attachment> attachments) {}
    public record TransferConfirmation(@NotBlank @Size(max = 512) String qrToken) {}
    public record UpdateAccount(@Size(max = 160) String name, String status, UUID assignedEmployeeId, UUID locationId) {}
    public record PayrollPayment(@NotNull UUID payrollId, @NotNull UUID employeeId,
                                 @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
                                 @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
                                 @NotNull UUID sourceAccountId, @NotBlank @Size(max = 128) String idempotencyKey) {}
}
