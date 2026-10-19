package com.bank.fundtransfer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/** All bank settings from the "bank:" section of application.yml (replaces the AppConfig singleton). */
@ConfigurationProperties(prefix = "bank")
public record BankProperties(
        BigDecimal minimumBalance,
        BigDecimal perTransactionLimit,
        BigDecimal dailyLimit,
        long requestExpirySeconds,
        String currencySymbol,
        String adminUsername,
        String adminPassword,
        Otp otp) {

    public record Otp(boolean fixed, String fixedValue, long expirySeconds, int maxAttempts) {
    }
}