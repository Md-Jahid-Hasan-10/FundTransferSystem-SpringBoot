package com.bank.fundtransfer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** An operation that is still waiting for OTP verification and confirmation. */
@Entity
@Table(name = "transaction_requests")
public class TransactionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_code", nullable = false, unique = true, length = 20)
    private String requestCode;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @ManyToOne(optional = false)
    @JoinColumn(name = "source_account_id", nullable = false)
    private Account source;

    @ManyToOne
    @JoinColumn(name = "destination_account_id")                      // empty for withdrawals
    private Account destination;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestStatus status = RequestStatus.PENDING_OTP;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected TransactionRequest() {
        // required by JPA
    }

    public TransactionRequest(String requestCode, Customer customer, TransactionType type, Account source,
                              Account destination, BigDecimal amount, BigDecimal fee,
                              Instant createdAt, Instant expiresAt) {
        this.requestCode = requestCode;
        this.customer = customer;
        this.type = type;
        this.source = source;
        this.destination = destination;
        this.amount = amount;
        this.fee = fee;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public BigDecimal getTotalDeduction() {
        return amount.add(fee);
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public Long getId() { return id; }
    public String getRequestCode() { return requestCode; }
    public Customer getCustomer() { return customer; }
    public TransactionType getType() { return type; }
    public Account getSource() { return source; }
    public Account getDestination() { return destination; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getFee() { return fee; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}