package q.qedet.warehouse.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class ReturnDtos {
 private ReturnDtos() {}
 public record ItemInput(UUID productId, String qrReference, @NotNull @DecimalMin("0.0001") BigDecimal quantity, String condition, String handlingNotes) {}
 public record CustomerCreate(@NotBlank String salesOrderReference, @NotBlank String receiptReference, @NotBlank String reason, @NotEmpty List<@Valid ItemInput> items) {}
 public record SupplierCreate(@NotBlank String reason, @NotEmpty List<@Valid ItemInput> items) {}
 public record DamageCreate(@NotBlank String reason, @NotNull UUID siteZoneId, @NotEmpty List<@Valid ItemInput> items) {}
 public record ScanRequest(@NotBlank String qrReference, UUID returnId, @NotBlank String purpose) {}
 public record ReceiptValidationRequest(@NotBlank String receiptQrReference, @NotBlank String productQrReference) {}
 public record ReceivingScanRequest(@NotBlank String qrReference, @NotNull @DecimalMin("0.0001") BigDecimal quantity, String notes) {}
 public record DecisionRequest(@NotBlank String decision, @NotNull UUID itemId, @NotNull @DecimalMin("0.0001") BigDecimal quantity, String notes, @NotNull UUID siteZoneId) {}
 public record FinancialRequest(@NotBlank String currency) {}
 public record SupplierInspectionRequest(@NotNull @DecimalMin("0.0001") BigDecimal quantity, @NotNull UUID siteZoneId) {}
 public record ItemResponse(UUID id, UUID productId, String qrReference, BigDecimal requestedQuantity, BigDecimal receivedQuantity, BigDecimal returnedQuantity, BigDecimal inspectionQuantity, BigDecimal acceptedQuantity, String status, UUID inspectionLocation, String condition, String handlingNotes) {}
 public record ReturnResponse(UUID id, String reference, String type, String status, String reason, String salesOrderReference, String receiptReference, UUID receivingOperationId, UUID financialRequestId, Instant createdAt, List<ItemResponse> items) {}
 public record PageResponse(List<ReturnResponse> items, long total, int page, int size) {}
 public record Summary(long pendingReceipt, long pendingInspection, long accepted, long purchaseReturns, long quarantine) {}
 public record ScanResponse(boolean verified, boolean matchesExpectedRequest, UUID productId, String productName, String sku, String productType, String baseUnit, BigDecimal availableQuantity, String originalSaleOrPurchaseOrder, String receivingOperation, String supplier) {}
 public record ReceiptValidationResponse(String salesOrderReference, String salesOrderLineId, BigDecimal unitPrice, String currency, BigDecimal soldQuantity, BigDecimal previouslyReturnedQuantity, BigDecimal remainingReturnableQuantity) {}
 public record ReceivingResponse(UUID operationId, String status) {}
 public record FinancialResponse(UUID financialRequestId, String reference, BigDecimal amount, String currency, String status) {}
}
