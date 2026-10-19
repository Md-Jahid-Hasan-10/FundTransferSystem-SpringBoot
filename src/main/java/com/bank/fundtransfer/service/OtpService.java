package com.bank.fundtransfer.service;

import com.bank.fundtransfer.config.BankProperties;
import com.bank.fundtransfer.entity.OtpRecord;
import com.bank.fundtransfer.entity.TransactionRequest;
import com.bank.fundtransfer.exception.InvalidOtpException;
import com.bank.fundtransfer.exception.OtpExpiredException;
import com.bank.fundtransfer.otp.OtpGenerator;
import com.bank.fundtransfer.repository.OtpRecordRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Creates and checks OTPs. Only a hash of the OTP is stored in the database.
 * This class is deliberately NOT @Transactional: it runs inside the caller's transaction, so
 * the attempt counter it updates is saved together with the caller's work.
 */
@Service
public class OtpService {
    private final OtpGenerator generator;
    private final OtpRecordRepository otpRecords;
    private final PasswordEncoder passwordEncoder;
    private final BankProperties properties;
    private final Clock clock;

    public OtpService(OtpGenerator generator, OtpRecordRepository otpRecords,
                      PasswordEncoder passwordEncoder, BankProperties properties, Clock clock) {
        this.generator = generator;
        this.otpRecords = otpRecords;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    /** Creates an OTP for one request and returns it (so it can be sent to the customer). */
    public String generateOtp(TransactionRequest request) {
        String otp = generator.generate();
        Instant expiresAt = Instant.now(clock).plusSeconds(properties.otp().expirySeconds());
        otpRecords.save(new OtpRecord(request, passwordEncoder.encode(otp), expiresAt));
        return otp;
    }

    /** Returns normally if the OTP is correct; otherwise throws. */
    public void verify(TransactionRequest request, String submittedOtp) {
        OtpRecord otpRecord = otpRecords.findByRequest(request).orElse(null);
        if (otpRecord == null || otpRecord.isUsed()) {
            throw new InvalidOtpException("No active OTP for this request.", 0);
        }
        if (Instant.now(clock).isAfter(otpRecord.getExpiresAt())) {
            throw new OtpExpiredException("OTP has expired.");
        }
        int maxAttempts = properties.otp().maxAttempts();
        if (otpRecord.getAttempts() >= maxAttempts) {
            throw new InvalidOtpException("Maximum OTP attempts exceeded.", 0);
        }
        if (submittedOtp != null && passwordEncoder.matches(submittedOtp, otpRecord.getOtpHash())) {
            otpRecord.markUsed();
            return;
        }
        otpRecord.incrementAttempts();
        int remaining = maxAttempts - otpRecord.getAttempts();
        String message = remaining == 0
                ? "Incorrect OTP. No attempts left."
                : "Incorrect OTP. Attempts left: " + remaining + ".";
        throw new InvalidOtpException(message, remaining);
    }
}