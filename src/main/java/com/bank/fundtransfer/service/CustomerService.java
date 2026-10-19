package com.bank.fundtransfer.service;

import com.bank.fundtransfer.config.BankProperties;
import com.bank.fundtransfer.entity.Account;
import com.bank.fundtransfer.entity.AccountRole;
import com.bank.fundtransfer.entity.Customer;
import com.bank.fundtransfer.exception.BankException;
import com.bank.fundtransfer.repository.AccountRepository;
import com.bank.fundtransfer.repository.CustomerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bank.fundtransfer.exception.AuthenticationFailedException;
import java.util.List;

/** Registration, login and account opening. */
@Service
@Transactional
public class CustomerService {
    private final CustomerRepository customers;
    private final AccountRepository accounts;
    private final CodeGeneratorService codes;
    private final PasswordEncoder passwordEncoder;
    private final BankProperties properties;

    public CustomerService(CustomerRepository customers, AccountRepository accounts,
                           CodeGeneratorService codes, PasswordEncoder passwordEncoder,
                           BankProperties properties) {
        this.customers = customers;
        this.accounts = accounts;
        this.codes = codes;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    /** Registers a customer and opens their first account. */
    public Customer register(String name, String email, String phone, String pin) {
        if (isBlank(name) || isBlank(email) || isBlank(phone)) {
            throw new BankException("Name, email and phone are required.");
        }
        if (pin == null || pin.length() < 4) {
            throw new BankException("PIN must be at least 4 characters.");
        }
        if (customers.existsByEmail(email)) {
            throw new BankException("This email is already registered.");
        }
        if (customers.existsByPhone(phone)) {
            throw new BankException("This phone number is already registered.");
        }
        Customer customer = customers.save(new Customer(codes.nextCustomerCode(), name.trim(),
                email.trim(), phone.trim(), passwordEncoder.encode(pin)));
        openAccount(customer);
        return customer;
    }

    /** Same message for a wrong ID and a wrong PIN, so attackers learn nothing. */
    @Transactional(readOnly = true)
    public Customer login(String customerCode, String pin) {
        Customer customer = customers.findByCustomerCode(customerCode)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid customer ID or PIN."));
        if (pin == null || !passwordEncoder.matches(pin, customer.getPinHash())) {
            throw new AuthenticationFailedException("Invalid customer ID or PIN.");
        }
        return customer;
    }

    public Account openAccount(Customer customer) {
        return accounts.save(new Account(codes.nextAccountNumber(), customer,
                AccountRole.CUSTOMER, properties.minimumBalance()));
    }

    @Transactional(readOnly = true)
    public Customer findByCode(String customerCode) {
        return customers.findByCustomerCode(customerCode)
                .orElseThrow(() -> new BankException("Customer not found."));
    }

    @Transactional(readOnly = true)
    public List<Account> getAccounts(Customer customer) {
        return accounts.findByOwner(customer);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}