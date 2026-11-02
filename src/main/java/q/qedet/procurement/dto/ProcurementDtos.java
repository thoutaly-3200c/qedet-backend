package q.qedet.procurement.dto;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List; import java.util.UUID;

public final class ProcurementDtos {
    private ProcurementDtos() {}
    public record ProductRequest(@NotBlank String sku,@NotBlank String name,String brand,String category,String type,String unit,String barcode,
                                  BigDecimal purchasePrice,BigDecimal sellingPrice,BigDecimal vatRate,BigDecimal minStock,BigDecimal maxStock) {}
    public record SupplierRequest(@NotBlank String name,String contact,String email,String phone,String category,String city,String country,String address,
                                   String paymentTerms,Integer leadTimeDays,String currency,BigDecimal rating) {}
    public record RequestItem(String productId, String productName, String name, String itemCategory, String productType,
                              String description, @Positive BigDecimal qty, @Positive BigDecimal quantity, String unit,
                              @DecimalMin("0.0") BigDecimal unitPrice, String currency, String notes) {}
    public record PurchaseRequestRequest(@NotBlank String requestType, @NotBlank @Size(max = 255) String title,
                                         @NotBlank @Size(max = 2000) String reason, String department,
                                         String sourceDomain, String sourceReference, LocalDate neededBy, String priority,
                                         @Size(max = 2000) String notes, @Size(max = 1000) String attachmentReference,
                                         @Pattern(regexp = "[A-Za-z]{3}") String currency,
                                         @NotEmpty List<@Valid RequestItem> items) {}
    public record PurchaseRequestUpdateRequest(@NotBlank @Size(max = 255) String title,
                                               @NotBlank @Size(max = 2000) String reason, LocalDate neededBy,
                                               String priority, @Size(max = 2000) String notes,
                                               @Size(max = 1000) String attachmentReference,
                                               @NotEmpty List<@Valid RequestItem> items) {}
    public record PurchaseOrderItemRequest(
            @NotNull UUID purchaseRequestItemId,
            @NotNull UUID supplierId,
            LocalDate orderDate,
            LocalDate expectedDeliveryDate,
            @NotBlank
            @Pattern(regexp = "[A-Za-z]{3}")
            String currency,
            String paymentTerms,
            String paymentMethod,
            String paymentTiming,

            @NotBlank
            String productType,

            @NotNull
            @Positive
            BigDecimal quantity,

            @NotBlank
            String unit,

            @NotNull
            @DecimalMin("0.00")
            BigDecimal totalPrice,

            @DecimalMin("0.00")
            @DecimalMax("100.00")
            BigDecimal discountPercent,

            @DecimalMin("0.00")
            BigDecimal discountAmount,

            @DecimalMin("0.00")
            BigDecimal taxAmount,

            @DecimalMin("0.00")
            BigDecimal shippingAmount
    ) {}
    public record PurchaseOrderRequest(@NotNull UUID purchaseRequestId, @Size(max = 2000) String notes,
                                       @NotEmpty List<@Valid PurchaseOrderItemRequest> items) {}
    public record StatusTransitionRequest(@NotBlank String status) {}
}
