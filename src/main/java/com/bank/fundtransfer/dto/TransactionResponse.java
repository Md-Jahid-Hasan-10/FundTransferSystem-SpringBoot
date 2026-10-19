package com.bank.fundtransfer.dto;

import com.bank.fundtransfer.entity.Transaction;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(String transactionCode, String type, String fromAccount, String toAccount,
                                  BigDecimal amount, BigDecimal fee, String status, Instant createdAt) {

    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getTransactionCode(),
                tx.getType().name(),
                tx.getSource() == null ? null : tx.getSource().getAccountNumber(),
                tx.getDestination() == null ? null : tx.getDestination().getAccountNumber(),
                tx.getAmount(),
                tx.getFee(),
                tx.getStatus().name(),
                tx.getCreatedAt());
    }
}