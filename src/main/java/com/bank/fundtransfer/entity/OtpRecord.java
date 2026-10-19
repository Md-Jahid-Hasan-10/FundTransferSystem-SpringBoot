package com.bank.fundtransfer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** One OTP for one transaction request. Only a hash of the OTP is stored. */
@Entity
@Table(name = "otp_records")
public class OtpRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private TransactionRequest request;

    @Column(name = "otp_hash", nullable = false, length = 100)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private boolean used = false;

    protected OtpRecord() {
        // required by JPA
    }

    public OtpRecord(TransactionRequest request, String otpHash, Instant expiresAt) {
        this.request = request;
        this.otpHash = otpHash;
        this.expiresAt = expiresAt;
    }

    public void incrementAttempts() {
        attempts++;
    }

    public void markUsed() {
        used = true;
    }

    public Long getId() { return id; }
    public TransactionRequest getRequest() { return request; }
    public String getOtpHash() { return otpHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public int getAttempts() { return attempts; }
    public boolean isUsed() { return used; }
}