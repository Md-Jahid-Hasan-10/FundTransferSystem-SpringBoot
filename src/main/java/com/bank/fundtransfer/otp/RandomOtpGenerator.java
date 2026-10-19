package com.bank.fundtransfer.otp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Used when bank.otp.fixed is false. */
@Component
@ConditionalOnProperty(name = "bank.otp.fixed", havingValue = "false", matchIfMissing = true)
public class RandomOtpGenerator implements OtpGenerator {
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}