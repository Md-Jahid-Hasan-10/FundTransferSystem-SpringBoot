package com.bank.fundtransfer.dto;

import java.math.BigDecimal;

public record LimitsResponse(BigDecimal perTransactionLimit, BigDecimal dailyLimit, BigDecimal usedToday,
                             BigDecimal remainingToday, BigDecimal minimumBalance) {
}