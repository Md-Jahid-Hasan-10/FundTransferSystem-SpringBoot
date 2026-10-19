package com.bank.fundtransfer.dto;

import com.bank.fundtransfer.entity.Account;

import java.math.BigDecimal;

public record AccountResponse(String accountNumber, String ownerName, String role, String status,
                              BigDecimal balance, BigDecimal minimumBalance, BigDecimal availableToSpend) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountNumber(),
                account.getOwner() == null ? "BANK" : account.getOwner().getName(),
                account.getRole().name(),
                account.getStatus().name(),
                account.getBalance(),
                account.getMinimumBalance(),
                account.getAvailableToSpend());
    }
}