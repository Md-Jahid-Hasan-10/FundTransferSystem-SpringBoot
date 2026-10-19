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

/** Final record of an operation. It has no setters, so it cannot be changed after it is saved. */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_code", nullable = false, unique = true, updatable = false, length = 20)
    private String transactionCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private TransactionType type;

    @ManyToOne
    @JoinColumn(name = "source_account_id", updatable = false)       // empty for deposits
    private Account source;

    @ManyToOne
    @JoinColumn(name = "destination_account_id", updatable = false)  // empty for withdrawals
    private Account destination;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal fee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Transaction() {
        // required by JPA
    }

    public Transaction(String transactionCode, TransactionType type, Account source, Account destination,
                       BigDecimal amount, BigDecimal fee, TransactionStatus status, Instant createdAt) {
        this.transactionCode = transactionCode;
        this.type = type;
        this.source = source;
        this.destination = destination;
        this.amount = amount;
        this.fee = fee;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getTransactionCode() { return transactionCode; }
    public TransactionType getType() { return type; }
    public Account getSource() { return source; }
    public Account getDestination() { return destination; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getFee() { return fee; }
    public TransactionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}