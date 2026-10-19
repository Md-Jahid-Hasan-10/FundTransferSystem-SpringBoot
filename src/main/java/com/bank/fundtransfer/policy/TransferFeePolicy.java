package com.bank.fundtransfer.policy;

import java.math.BigDecimal;

public interface TransferFeePolicy {
    BigDecimal calculateFee(BigDecimal amount);
}