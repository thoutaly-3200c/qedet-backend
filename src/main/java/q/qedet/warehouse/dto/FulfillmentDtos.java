package q.qedet.warehouse.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;

public final class FulfillmentDtos {
    private FulfillmentDtos() {}
    public record Allocation(@NotNull UUID sourceZoneId, @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal quantity) {}
    public record Line(@NotNull UUID productId, @NotEmpty List<@Valid Allocation> allocations) {}
    public record Confirm(@NotNull UUID salesHallId,
                          @NotNull UUID destinationZoneId, @NotEmpty List<@Valid Line> lines) {}
    public record Decision(@Size(max = 1000) String notes) {}
    public record ReturnLine(@NotNull UUID productId, @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal quantity) {}
    public record ReturnReceipt(@NotNull UUID warehouseZoneId, @NotEmpty List<@Valid ReturnLine> lines) {}
}
