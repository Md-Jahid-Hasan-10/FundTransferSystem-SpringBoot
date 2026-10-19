package com.bank.fundtransfer.policy;

import java.math.BigDecimal;

public interface TransactionLimitPolicy {
    /** Throws TransactionLimitExceededException if the amount is not allowed. */
    void check(BigDecimal amount, BigDecimal alreadyUsedToday);
}