package q.qedet.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.time.Instant;

public final class SalesDtos {
    private SalesDtos() {}
    public record QuantityItem(@NotNull UUID productId, @NotNull @DecimalMin(value = "0.01") BigDecimal quantity, String serialReference, String batchReference) {}
    public record StockRequestCreate(
            @Size(max = 2000) String notes,
            @NotEmpty List<@Valid QuantityItem> items
    ) {}
    public record ReceivingLine(@NotNull UUID productId,
                                @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal acceptedQuantity,
                                @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal rejectedQuantity,
                                 UUID salesHallZoneId,  UUID warehouseZoneId) {}
    public record ReceivingConfirmation(@NotEmpty List<@Valid ReceivingLine> items, @Size(max = 1000) String notes) {}
    public record PlaceStock(@NotNull UUID salesHallId, @NotNull UUID productId, @NotNull UUID locationId, @NotNull @DecimalMin(value = "0.01") BigDecimal quantity) {}
    public record PriceUpdate(
            @NotNull UUID productId,

            @NotNull
            @DecimalMin(value = "0.00", inclusive = false)
            @Digits(integer = 12, fraction = 2)
            BigDecimal currentPrice,

            @Size(max = 1000)
            String reason
    ) {}    public record PackageCreate(@NotNull UUID salesHallId, @NotNull UUID productId, @NotNull @DecimalMin(value = "0.01") BigDecimal weight, @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal unitPrice) {}
    public record ReturnCreate(@NotNull UUID salesHallId, @NotNull UUID destinationWarehouseId, @Size(max = 1000) String reason, @Size(max = 2000) String notes, @NotEmpty List<@Valid QuantityItem> items) {}
    public record CashierCashbox(UUID id, String name) {}
    public record PosAssignmentRequest(@NotBlank @Size(max = 40) String code, @NotNull UUID salesHallId, boolean active) {}
    public record PosAssignmentResponse(UUID posId, String posCode, UUID salesHallId, UUID cashierId, boolean active) {}
    public record CashierAssignment(UUID posId, String posCode, UUID salesHallId,
                                    String salesHallName, UUID cashierId, String cashierName,
                                    CashierCashbox cashbox, String status, String currency) {}
    public record CashierScanRequest(@NotBlank String code, @NotNull UUID salesHallId) {}
    public record CashierScanResponse(UUID productId, String sku, String name, String type,
                                      String unit, BigDecimal unitPrice, String currency,
                                      String serialNumber, UUID packageId,
                                      BigDecimal packageWeight, BigDecimal packageTotalPrice) {}
    public record PosQuoteRequest(@NotNull UUID salesHallId, @NotEmpty List<@Valid PosQuoteRequestLine> lines) {}
    public record PosQuoteRequestLine(UUID productId, @DecimalMin(value = "0.01") @Digits(integer = 12, fraction = 3) BigDecimal quantity,
                                      @Size(max = 160) String serialNumber, UUID packageId) {}
    public record PosQuoteLine(String lineKey, UUID productId, UUID packageId, String name, String sku,
                               BigDecimal quantity, BigDecimal unitPrice, BigDecimal lineTotal, BigDecimal taxAmount,
                               String serialNumber) {}
    public record PosQuoteResponse(String currency, List<PosQuoteLine> lines, BigDecimal subtotal,
                                   BigDecimal discountTotal, BigDecimal taxTotal, BigDecimal total,
                                   String quoteToken, Instant expiresAt) {}
    public record PosPaymentRequest(@NotBlank String method, @NotNull UUID cashboxId,
                                    @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal amountReceived,
                                    @Size(max = 160) String reference) {}
    public record PosSaleRequest(@NotBlank String quoteToken, @NotNull @Valid PosPaymentRequest payment) {}
    public record PosSaleResponse(UUID saleId, String transactionCode, String status, String paymentStatus,
                                  String currency, BigDecimal subtotal, BigDecimal discountTotal,
                                  BigDecimal taxTotal, BigDecimal total, BigDecimal amountReceived,
                                  BigDecimal change, Instant completedAt, String receiptUrl) {}
    public record PosReceiptResponse(UUID saleId, String transactionCode, Instant completedAt,
                                     UUID salesHallId, String salesHallName, UUID cashierId, String cashierName,
                                     String currency, List<PosQuoteLine> items, BigDecimal subtotal,
                                     BigDecimal discountTotal, BigDecimal taxTotal, BigDecimal total,
                                     String paymentMethod, BigDecimal amountReceived, BigDecimal change) {}
}
