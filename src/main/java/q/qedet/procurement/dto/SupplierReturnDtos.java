package q.qedet.procurement.dto;
import java.math.BigDecimal;
import java.util.*;
import java.time.Instant;
public final class SupplierReturnDtos {
 private SupplierReturnDtos() {}
 public record ReviewLine(UUID requestItemId,UUID purchaseOrderItemId,UUID receivingOperationItemId,BigDecimal approvedQuantity) {}
 public record Review(List<ReviewLine> items) {}
 public record RequestItem(UUID id,UUID productId,BigDecimal requestedQuantity,String status) {}
 public record Request(UUID id,String reference,String status,String reason,List<RequestItem> items) {}
 public record History(UUID supplierId,String supplierName,UUID purchaseOrderId,String purchaseOrderNumber,UUID purchaseOrderItemId,UUID receivingOperationId,String receivingRequestNumber,UUID receivingOperationItemId,BigDecimal orderedQuantity,BigDecimal receivedQuantity,BigDecimal acceptedQuantity,BigDecimal rejectedQuantity,String currency,String unit,BigDecimal unitCost,String availabilityNote) {}
 public record Line(UUID id,UUID requestItemId,UUID productId,String sku,UUID purchaseOrderItemId,String purchaseOrderNumber,UUID receivingOperationId,String receivingRequestNumber,UUID receivingOperationItemId,BigDecimal approvedQuantity,BigDecimal dispatchedQuantity,BigDecimal supplierAcceptedQuantity,BigDecimal supplierRejectedQuantity,BigDecimal unitCost,String currency) {}
 public record Operation(UUID id,String reference,UUID requestId,String requestReference,String reason,UUID supplierId,String supplierName,String status,String settlementType,String settlementReference,UUID financialRequestId,List<Line> items) {}
 public record Timeline(String event,String status,Instant timestamp,String notes) {}
 public record DispatchEvent(String reference,String trackingReference,UUID employeeId,Instant dispatchedAt,UUID operationItemId,UUID sourceSiteId,UUID sourceZoneId,BigDecimal quantity) {}
 public record FinancialSummary(String currency,BigDecimal approvedValue,BigDecimal dispatchedValue,BigDecimal acceptedValue,BigDecimal rejectedValue,BigDecimal refundAmount,BigDecimal creditNoteAmount,BigDecimal replacementValue,String settlementStatus) {}
 public record Detail(Operation returnInformation,List<Timeline> lifecycleTimeline,List<DispatchEvent> dispatch,FinancialSummary financialSummary) {}
 public record DispatchLine(UUID operationItemId,UUID sourceZoneId,BigDecimal quantity) {}
 public record Dispatch(String reference,String trackingReference,List<DispatchLine> items) {}
 public record AcceptanceLine(UUID operationItemId,BigDecimal acceptedQuantity,BigDecimal rejectedQuantity) {}
 public record Acceptance(List<AcceptanceLine> items) {}
 public record Settlement(String type,String reference,String notes) {}
}
