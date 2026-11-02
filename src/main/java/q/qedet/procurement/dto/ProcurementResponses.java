package q.qedet.procurement.dto;
import java.math.BigDecimal; import java.time.Instant; import java.time.LocalDate; import java.util.List; import java.util.UUID;
public final class ProcurementResponses {
 private ProcurementResponses(){}
 public record ProductResponse(UUID id,String sku,String name,String brand,String category,String type,String unit,String barcode,BigDecimal purchasePrice,BigDecimal sellingPrice,BigDecimal vatRate,BigDecimal minStock,BigDecimal maxStock,String status){}
 public record SupplierResponse(UUID id,String name,String contact,String email,String phone,String category,String city,String country,String address,String paymentTerms,Integer leadTimeDays,String currency,BigDecimal rating,BigDecimal onTimeRate,BigDecimal defectRate,Integer totalOrders,BigDecimal spend,String status){}
 public record PurchaseRequestItemResponse(UUID id,String productId,String productName,String name,String sku,
                                           String itemCategory,String productType,String description,BigDecimal qty,
                                           BigDecimal quantity,String unit,BigDecimal unitPrice,String currency,String notes){}
 public record PurchaseRequestResponse(UUID id,String number,UUID requestedBy,String requestType,String title,String reason,
                                       String department,String sourceDomain,String sourceReference,LocalDate date,
                                       LocalDate neededBy,String priority,String status,String procurementStatus,String notes,
                                       String attachmentReference,List<PurchaseRequestItemResponse> items,
                                       BigDecimal totalEstimate,String currency,Instant createdAt,Instant updatedAt){}
 public record ProductSummary(UUID id, String sku, String name, String unit, String description,
                              boolean catalogProduct) {}

 public record PurchaseOrderItemResponse(
         UUID id,
         UUID purchaseRequestItemId,
         String productType,
         ProductSummary product,
         UUID supplierId,
         String supplier,
         LocalDate orderDate,
         LocalDate expectedDeliveryDate,
         String currency,
         String paymentTerms,
         String paymentMethod,
         String paymentTiming,
         BigDecimal quantity,
         String unit,
         BigDecimal totalPrice,
         BigDecimal discountPercent,
         BigDecimal discountAmount,
         BigDecimal taxAmount,
         BigDecimal shippingAmount,
         String orderStatus,
         String receivingStatus,
         UUID createdBy
 ) {}

 public record CurrencyTotal(String currency, BigDecimal total) {}

 public record PurchaseOrderResponse(
         UUID id,
         String number,
         UUID purchaseRequestId,
         String purchaseRequestNumber,
         String status,
         String notes,
         UUID createdBy,
         Instant createdAt,
         Instant updatedAt,
         List<PurchaseOrderItemResponse> items,
         List<CurrencyTotal> totalsByCurrency
 ) {}

}
