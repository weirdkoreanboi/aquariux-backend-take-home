package com.aquariux.technical.assessment.trade.dto.request;

import com.aquariux.technical.assessment.trade.enums.TradeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TradeRequest {
    @NotNull(message = "userId is required")
    private Long userId;

    @NotBlank(message = "pairName is required")
    private String pairName;

    @NotNull(message = "tradeType is required")
    private TradeType tradeType;

    @NotNull(message = "quantity is required")
    @DecimalMin(value = "0.00000001", message = "quantity must be greater than zero")
    @Digits(integer = 12, fraction = 8, message = "quantity must have at most 12 integer and 8 decimal digits")
    private BigDecimal quantity;
}
