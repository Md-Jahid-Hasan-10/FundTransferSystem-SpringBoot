package com.bank.fundtransfer.controller;

import com.bank.fundtransfer.dto.AccountResponse;
import com.bank.fundtransfer.dto.AuthHeaders;
import com.bank.fundtransfer.dto.TransactionResponse;
import com.bank.fundtransfer.entity.Customer;
import com.bank.fundtransfer.service.AuthService;
import com.bank.fundtransfer.service.CustomerService;
import com.bank.fundtransfer.service.FundTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "Your accounts and transaction history")
public class AccountController {
    private final AuthService authService;
    private final CustomerService customerService;
    private final FundTransferService transferService;

    public AccountController(AuthService authService, CustomerService customerService,
                             FundTransferService transferService) {
        this.authService = authService;
        this.customerService = customerService;
        this.transferService = transferService;
    }

    @GetMapping
    @Operation(summary = "List my accounts")
    public List<AccountResponse> myAccounts(
            @RequestHeader(AuthHeaders.CUSTOMER_CODE) String customerCode,
            @RequestHeader(AuthHeaders.CUSTOMER_PIN) String pin) {
        Customer customer = authService.authenticateCustomer(customerCode, pin);
        return customerService.getAccounts(customer).stream().map(AccountResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a new account")
    public AccountResponse openAccount(
            @RequestHeader(AuthHeaders.CUSTOMER_CODE) String customerCode,
            @RequestHeader(AuthHeaders.CUSTOMER_PIN) String pin) {
        Customer customer = authService.authenticateCustomer(customerCode, pin);
        return AccountResponse.from(customerService.openAccount(customer));
    }

    @GetMapping("/{accountNumber}/transactions")
    @Operation(summary = "Transaction history of one of my accounts (newest first)")
    public List<TransactionResponse> history(
            @RequestHeader(AuthHeaders.CUSTOMER_CODE) String customerCode,
            @RequestHeader(AuthHeaders.CUSTOMER_PIN) String pin,
            @PathVariable String accountNumber) {
        Customer customer = authService.authenticateCustomer(customerCode, pin);
        return transferService.getHistory(customer, accountNumber).stream()
                .map(TransactionResponse::from).toList();
    }
}