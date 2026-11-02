package q.qedet.warehouse.dto;
import jakarta.validation.constraints.*; import q.qedet.product.entity.*; import java.math.BigDecimal; import java.time.Instant; import java.util.*;
public final class ReceivingDtos {
 private ReceivingDtos(){}
 public enum SupplierQrStatus { REGISTRATION_REQUIRED, MATCHES_PRODUCT }
 public enum SupplierQrDecision { LINK_EXISTING_PRODUCT, CREATE_DIFFERENT_PRODUCT }
 public record StartOperationRequest(String notes, UUID warehouseId, UUID deliveryLocationId) { public StartOperationRequest(String notes){this(notes,null,null);} }
 public record SerialIdentityRequest(@NotBlank String serialNumber, String supplierQrIdentifier, boolean accepted) {}
 public record ZoneAssignmentRequest(@NotNull UUID zoneId, @NotNull @DecimalMin("0.0001") BigDecimal quantity) {}
 public record OperationItemRequest(@NotNull UUID receivingRequestItemId,@NotNull @DecimalMin("0.0001") BigDecimal receivedQuantity,@NotNull @DecimalMin("0") BigDecimal acceptedQuantity,@NotNull @DecimalMin("0") BigDecimal rejectedQuantity,String notes,List<ZoneAssignmentRequest> zoneAssignments,List<String> serialNumbers,List<SerialIdentityRequest> serialIdentities,String supplierQrIdentifier) {
   public OperationItemRequest(UUID item,BigDecimal received,BigDecimal accepted,BigDecimal rejected,String notes,List<ZoneAssignmentRequest> zones,List<String> serials){this(item,received,accepted,rejected,notes,zones,serials,null,null);}
 }
 public record ProductRegistrationRequest(@NotBlank String name,String description,UUID unitId,@NotNull ProductType productType,@NotNull ProductStatus productStatus,String organizationSku,ProductBarcodeType barcodeType) {}
 public record SupplierQrResolveRequest(@NotNull UUID receivingRequestItemId,@NotBlank String identifier,String serialNumber,UUID productId,ProductRegistrationRequest registration) { public SupplierQrResolveRequest(UUID receivingRequestItemId,String identifier,String serialNumber){this(receivingRequestItemId,identifier,serialNumber,null,null);} }
 public record SupplierQrDecisionRequest(@NotNull UUID receivingRequestItemId,@NotBlank String identifier,@NotNull SupplierQrDecision decision) {}
 public record SupplierQrResolveResponse(String status,String identifier,UUID receivingRequestItemId,UUID productId,String productName,String trackingType,String serialNumber,UUID existingQrId,UUID existingTargetId,String existingTargetKind,String procurementProductName,String procurementSku,String procurementProductType,String procurementBaseUnit,boolean decisionRequired) {
   public SupplierQrResolveResponse(String status,String identifier,UUID receivingRequestItemId,UUID productId,String productName,String trackingType,String serialNumber,UUID existingQrId,UUID existingTargetId,String existingTargetKind){this(status,identifier,receivingRequestItemId,productId,productName,trackingType,serialNumber,existingQrId,existingTargetId,existingTargetKind,null,null,null,null,false);}
   /** Compatibility names for clients that previously called the QR target an existing product. */
   public UUID existingProductId(){return existingTargetKind==null||"PRODUCT".equals(existingTargetKind)?existingTargetId:null;}
   public String existingProductName(){return existingProductId()==null?null:productName;}
 }
 public record RequestItemResponse(UUID id,UUID purchaseOrderItemId,UUID productId,String productName,String sku,String productType,String baseUnit,BigDecimal expectedQuantity,BigDecimal previouslyReceivedQuantity,BigDecimal acceptedQuantity,BigDecimal rejectedQuantity,BigDecimal inProgressQuantity,BigDecimal remainingQuantity) {}
 public record RequestResponse(UUID id,String requestNumber,UUID purchaseOrderId,String purchaseOrderNumber,UUID warehouseId,UUID deliveryLocationId,String status,List<RequestItemResponse> items) {}
 public record ZoneAssignmentResponse(UUID zoneId,BigDecimal quantity) {}
 public record OperationItemResponse(UUID id,UUID receivingRequestItemId,BigDecimal receivedQuantity,BigDecimal acceptedQuantity,BigDecimal rejectedQuantity,String notes,String supplierQrIdentifier,List<SerialIdentityRequest> serialIdentities,List<ZoneAssignmentResponse> zoneAssignments) {}
 public record OperationResponse(UUID id,UUID receivingRequestId,UUID warehouseEmployeeId,String warehouseEmployeeName,String status,Instant startedAt,Instant completedAt,String notes,List<OperationItemResponse> items) {}
 public record HistoryResponse(List<Object> items) {}
}
