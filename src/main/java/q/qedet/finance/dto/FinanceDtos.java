package q.qedet.finance.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class FinanceDtos {
    private FinanceDtos() {}
    public record CashBoxUpdateRequest(@NotBlank @Size(max = 160) String name, UUID locationId) {}
    public record PaymentRequestCreate(@NotBlank String requestType, @NotBlank String sourceDepartment,
                                       @NotBlank @Size(max = 160) String sourceReference,
                                       @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
                                       @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
                                       @NotBlank @Size(max = 2000) String reason, String supportingDocuments,
                                       UUID supplierId, UUID purchaseOrderId, UUID purchaseOrderItemId, String supplierInvoice, LocalDate dueDate) {}
    public record DecisionRequest(@Size(max = 2000) String reason) {}
    public record ExecutePaymentRequest(@NotNull UUID cashBoxId, @Size(max = 160) String reference,
                                        @Size(max = 2000) String description) {}
    public record CashTransferRequest(@NotNull UUID fromCashBoxId, @NotNull UUID toCashBoxId,
                                      @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
                                      @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
                                      @NotBlank @Size(max = 2000) String reason) {}
    public record ReconciliationRequest(@NotNull @DecimalMin(value = "0.00") BigDecimal physicalCount,
                                        @NotBlank @Size(max = 2000) String reason, String supportingInformation) {}
}
