package com.bank.fundtransfer.service;

import com.bank.fundtransfer.config.BankProperties;
import com.bank.fundtransfer.entity.Account;
import com.bank.fundtransfer.entity.AccountRole;
import com.bank.fundtransfer.entity.Customer;
import com.bank.fundtransfer.entity.Transaction;
import com.bank.fundtransfer.exception.AccountNotFoundException;
import com.bank.fundtransfer.exception.BankException;
import com.bank.fundtransfer.repository.AccountRepository;
import com.bank.fundtransfer.repository.CustomerRepository;
import com.bank.fundtransfer.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Operations for bank staff. Phase 6 replaces this simple login with real security. */
@Service
@Transactional
public class AdminService {
    private final CustomerRepository customers;
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final NotificationService notifications;
    private final BankProperties properties;

    @PersistenceContext
    private EntityManager entityManager;

    public AdminService(CustomerRepository customers, AccountRepository accounts,
                        TransactionRepository transactions, NotificationService notifications,
                        BankProperties properties) {
        this.customers = customers;
        this.accounts = accounts;
        this.transactions = transactions;
        this.notifications = notifications;
        this.properties = properties;
    }

    /** Returns normally if the credentials are correct; otherwise throws. */
    public void login(String username, String password) {
        if (!properties.adminUsername().equals(username) || !properties.adminPassword().equals(password)) {
            throw new BankException("Invalid admin username or password.");
        }
    }

    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        return customers.findAll();
    }

    @Transactional(readOnly = true)
    public List<Account> getAllAccounts() {
        return accounts.findAll();
    }

    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactions() {
        return transactions.findAll();
    }

    public void freezeAccount(String accountNumber) {
        Account account = findCustomerAccountForUpdate(accountNumber);
        if (account.isFrozen()) {
            throw new BankException("Account " + accountNumber + " is already frozen.");
        }
        account.freeze();
        notifications.notifyAccountStatusChanged(account, true);
    }

    public void unfreezeAccount(String accountNumber) {
        Account account = findCustomerAccountForUpdate(accountNumber);
        if (!account.isFrozen()) {
            throw new BankException("Account " + accountNumber + " is not frozen.");
        }
        account.unfreeze();
        notifications.notifyAccountStatusChanged(account, false);
    }

    private Account findCustomerAccountForUpdate(String accountNumber) {
        Account account = accounts.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account " + accountNumber + " was not found."));
        if (account.getRole() != AccountRole.CUSTOMER) {
            throw new BankException("The bank fee account cannot be frozen.");
        }
        entityManager.flush();
        entityManager.refresh(account, LockModeType.PESSIMISTIC_WRITE);
        return account;
    }
}