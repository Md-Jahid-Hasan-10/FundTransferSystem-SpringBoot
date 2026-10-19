package com.bank.fundtransfer.entity;

import com.bank.fundtransfer.exception.InsufficientFundsException;
import com.bank.fundtransfer.exception.InvalidAmountException;
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
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @ManyToOne
    @JoinColumn(name = "owner_id")          // empty for the bank's fee account
    private Customer owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccountRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = new BigDecimal("0.00");

    @Column(name = "minimum_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal minimumBalance;

    @Version
    private Long version;                   // extra protection against two updates at the same time

    protected Account() {
        // required by JPA
    }

    public Account(String accountNumber, Customer owner, AccountRole role, BigDecimal minimumBalance) {
        this.accountNumber = accountNumber;
        this.owner = owner;
        this.role = role;
        this.minimumBalance = minimumBalance.setScale(2, RoundingMode.HALF_UP);
    }

    // ---------- business rules (same as the CLI version) ----------
    public void deposit(BigDecimal amount) {
        requirePositive(amount);
        balance = balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        requirePositive(amount);
        if (!hasSufficientFunds(amount)) {
            throw new InsufficientFundsException("Insufficient funds in account " + accountNumber
                    + " (a minimum balance of " + minimumBalance.toPlainString() + " must remain).");
        }
        balance = balance.subtract(amount);
    }

    public boolean hasSufficientFunds(BigDecimal amount) {
        return balance.subtract(amount).compareTo(minimumBalance) >= 0;
    }

    public BigDecimal getAvailableToSpend() {
        return balance.subtract(minimumBalance).max(new BigDecimal("0.00"));
    }

    public boolean isOwnedBy(Customer customer) {
        return owner != null && owner.getCustomerCode().equals(customer.getCustomerCode());
    }

    public boolean hasSameOwnerAs(Account other) {
        return owner != null && other.owner != null
                && owner.getCustomerCode().equals(other.owner.getCustomerCode());
    }

    public void freeze() {
        status = AccountStatus.FROZEN;
    }

    public void unfreeze() {
        status = AccountStatus.ACTIVE;
    }

    public boolean isFrozen() {
        return status == AccountStatus.FROZEN;
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
    }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public Customer getOwner() { return owner; }
    public AccountRole getRole() { return role; }
    public AccountStatus getStatus() { return status; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getMinimumBalance() { return minimumBalance; }
}