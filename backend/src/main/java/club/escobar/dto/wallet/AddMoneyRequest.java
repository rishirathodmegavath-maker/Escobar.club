package club.escobar.dto.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AddMoneyRequest(
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amountInr,
        @Size(max = 500) String note
) {
}
