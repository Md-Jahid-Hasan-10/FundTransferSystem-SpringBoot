package com.bank.fundtransfer.policy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Transfers between a customer's own accounts are free. */
@Component("ownAccountTransferFeePolicy")
public class OwnAccountTransferFeePolicy implements TransferFeePolicy {

    @Override
    public BigDecimal calculateFee(BigDecimal amount) {
        return new BigDecimal("0.00");
    }
}