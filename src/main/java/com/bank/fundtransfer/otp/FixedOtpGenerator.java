package com.bank.fundtransfer.otp;

import com.bank.fundtransfer.config.BankProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Used while bank.otp.fixed is true (easy testing). */
@Component
@ConditionalOnProperty(name = "bank.otp.fixed", havingValue = "true")
public class FixedOtpGenerator implements OtpGenerator {
    private final String otp;

    public FixedOtpGenerator(BankProperties properties) {
        this.otp = properties.otp().fixedValue();
    }

    @Override
    public String generate() {
        return otp;
    }
}