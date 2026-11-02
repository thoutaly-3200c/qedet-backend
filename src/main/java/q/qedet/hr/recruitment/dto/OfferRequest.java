package q.qedet.hr.recruitment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OfferRequest(
        @NotBlank @Size(max = 255) String positionTitle,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal salary,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @NotNull @FutureOrPresent LocalDate startDate,
        @NotNull @FutureOrPresent LocalDate expiresOn,
        @Size(max = 4000) String terms
) {
}
