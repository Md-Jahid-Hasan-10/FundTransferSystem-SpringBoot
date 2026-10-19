package com.bank.fundtransfer;

import com.bank.fundtransfer.entity.Account;
import com.bank.fundtransfer.entity.AccountRole;
import com.bank.fundtransfer.entity.Customer;
import com.bank.fundtransfer.exception.BankException;
import com.bank.fundtransfer.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerServiceTest {

    @Autowired
    private CustomerService customerService;

    @Test
    void registerCreatesCustomerAndFirstAccount() {
        Customer customer = customerService.register("Carol", "carol@test.com", "01800000001", "4321");
        assertTrue(customer.getCustomerCode().startsWith("CUST"));
        assertNotEquals("4321", customer.getPinHash());          // the PIN must be stored as a hash

        List<Account> accounts = customerService.getAccounts(customer);
        assertEquals(1, accounts.size());
        assertEquals(AccountRole.CUSTOMER, accounts.get(0).getRole());
        assertEquals(0, new BigDecimal("100.00").compareTo(accounts.get(0).getMinimumBalance()));
    }

    @Test
    void loginWorksOnlyWithTheCorrectPin() {
        Customer customer = customerService.register("Carol", "carol@test.com", "01800000001", "4321");
        Customer loggedIn = customerService.login(customer.getCustomerCode(), "4321");
        assertEquals(customer.getCustomerCode(), loggedIn.getCustomerCode());

        assertThrows(BankException.class, () -> customerService.login(customer.getCustomerCode(), "0000"));
        assertThrows(BankException.class, () -> customerService.login("CUST9999999", "4321"));
    }

    @Test
    void duplicateEmailOrPhoneIsRejected() {
        customerService.register("Carol", "carol@test.com", "01800000001", "4321");
        assertThrows(BankException.class,
                () -> customerService.register("Dave", "carol@test.com", "01800000002", "4321"));
        assertThrows(BankException.class,
                () -> customerService.register("Dave", "dave@test.com", "01800000001", "4321"));
    }

    @Test
    void shortPinIsRejected() {
        assertThrows(BankException.class,
                () -> customerService.register("Dave", "dave@test.com", "01800000002", "12"));
    }
}