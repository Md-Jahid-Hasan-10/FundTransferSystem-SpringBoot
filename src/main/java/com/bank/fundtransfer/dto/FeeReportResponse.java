package com.bank.fundtransfer.dto;

import java.math.BigDecimal;
import java.util.List;

public record FeeReportResponse(String feeAccountNumber, BigDecimal totalFeesCollected,
                                int transfersWithFee, List<TransactionResponse> transfers) {
}