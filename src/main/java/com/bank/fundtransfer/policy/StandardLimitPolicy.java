package com.bank.fundtransfer.policy;

import com.bank.fundtransfer.config.BankProperties;
import com.bank.fundtransfer.exception.TransactionLimitExceededException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class StandardLimitPolicy implements TransactionLimitPolicy {
    private final BigDecimal perTransactionLimit;
    private final BigDecimal dailyLimit;

    public StandardLimitPolicy(BankProperties properties) {
        this.perTransactionLimit = properties.perTransactionLimit();
        this.dailyLimit = properties.dailyLimit();
    }

    @Override
    public void check(BigDecimal amount, BigDecimal alreadyUsedToday) {
        if (amount.compareTo(perTransactionLimit) > 0) {
            throw new TransactionLimitExceededException(
                    "Amount exceeds the per-transaction limit of " + perTransactionLimit.toPlainString() + ".");
        }
        BigDecimal remaining = dailyLimit.subtract(alreadyUsedToday).max(BigDecimal.ZERO);
        if (amount.compareTo(remaining) > 0) {
            throw new TransactionLimitExceededException("Daily limit exceeded. Daily limit: "
                    + dailyLimit.toPlainString() + ", used today: " + alreadyUsedToday.toPlainString()
                    + ", remaining: " + remaining.toPlainString() + ".");
        }
    }
}