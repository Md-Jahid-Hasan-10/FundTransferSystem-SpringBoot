package com.bank.fundtransfer.dto;

import com.bank.fundtransfer.entity.TransactionRequest;

import java.math.BigDecimal;
import java.time.Instant;

/** What the customer sees while a withdrawal or transfer waits for OTP and confirmation. */
public record TransactionRequestResponse(String requestCode, String type, String fromAccount, String toAccount,
                                         String receiverName, BigDecimal amount, BigDecimal fee,
                                         BigDecimal totalDeduction, String status, Instant expiresAt,
                                         String message) {

    public static TransactionRequestResponse from(TransactionRequest request, String message) {
        return new TransactionRequestResponse(
                request.getRequestCode(),
                request.getType().name(),
                request.getSource().getAccountNumber(),
                request.getDestination() == null ? null : request.getDestination().getAccountNumber(),
                request.getDestination() == null ? null : request.getDestination().getOwner().getName(),
                request.getAmount(),
                request.getFee(),
                request.getTotalDeduction(),
                request.getStatus().name(),
                request.getExpiresAt(),
                message);
    }
}