package q.qedet.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class LogisticsDtos {
    private LogisticsDtos() {}

    public record LineRequest(@NotBlank String productId, @NotNull @Positive BigDecimal quantity,
                              BigDecimal acceptedQuantity, BigDecimal rejectedQuantity,
                              String locationId, String lotNumber) {}

    public record RecordRequest(String code, String name, String productId, String supplierId,
                                String purchaseOrderId, String shipmentId, String locationId,
                                String fromLocationId, String toLocationId, String deliveryLocationId,
                                String reference, String reason, LocalDate date, LocalDate dueDate,
                                BigDecimal quantity, BigDecimal acceptedQuantity,
                                BigDecimal rejectedQuantity, String notes, List<LineRequest> lines) {}

    public record LocationRequest(@NotBlank String code, @NotBlank String name, String type,
                                  String parentCode, BigDecimal capacity, Boolean active) {}

    public record DeliveryLocationRequest(@NotBlank String name, String address, String city,
                                          String country, String contact, String phone, Boolean active) {}

    public record QuoteLine(@NotBlank String productId, @NotNull @Positive BigDecimal quantity,
                            @NotNull @Positive BigDecimal unitPrice) {}

    public record QuotationRequest(String supplierId, LocalDate validUntil, String currency,
                                   String notes, List<QuoteLine> lines) {}
}
