package com.bank.fundtransfer;

import com.bank.fundtransfer.entity.Account;
import com.bank.fundtransfer.entity.AccountRole;
import com.bank.fundtransfer.entity.Customer;
import com.bank.fundtransfer.repository.AccountRepository;
import com.bank.fundtransfer.repository.CustomerRepository;
import com.bank.fundtransfer.service.CodeGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every test rolls back at the end, so the test database stays clean. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PersistenceSmokeTest {

    @Autowired
    private CustomerRepository customers;

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private CodeGeneratorService codes;

    @Test
    void bankFeeAccountIsCreatedByTheMigration() {
        Account feeAccount = accounts.findFirstByRole(AccountRole.BANK_FEE_COLLECTION).orElseThrow();
        assertEquals("BANK-FEE-001", feeAccount.getAccountNumber());
        assertEquals(0, feeAccount.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void canSaveAndLoadACustomerAccount() {
        Customer customer = customers.save(new Customer(
                codes.nextCustomerCode(), "Smoke Test", "smoke@example.com", "01999999999", "not-a-real-hash"));
        Account account = accounts.save(new Account(
                codes.nextAccountNumber(), customer, AccountRole.CUSTOMER, new BigDecimal("100.00")));

        account.deposit(new BigDecimal("500.00"));
        accounts.flush();

        Account loaded = accounts.findByAccountNumber(account.getAccountNumber()).orElseThrow();
        assertEquals(0, loaded.getBalance().compareTo(new BigDecimal("500.00")));
        assertEquals(0, loaded.getAvailableToSpend().compareTo(new BigDecimal("400.00")));
        assertTrue(loaded.isOwnedBy(customer));
        assertEquals(1, accounts.findByOwner(customer).size());
    }

    @Test
    void codeGeneratorProducesDifferentCodes() {
        String first = codes.nextCustomerCode();
        String second = codes.nextCustomerCode();
        assertTrue(first.startsWith("CUST"));
        assertNotEquals(first, second);
    }
}