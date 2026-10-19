package com.bank.fundtransfer.controller;

import com.bank.fundtransfer.dto.AccountResponse;
import com.bank.fundtransfer.dto.CustomerResponse;
import com.bank.fundtransfer.dto.LoginRequest;
import com.bank.fundtransfer.dto.RegisterRequest;
import com.bank.fundtransfer.dto.RegistrationResponse;
import com.bank.fundtransfer.entity.Account;
import com.bank.fundtransfer.entity.Customer;
import com.bank.fundtransfer.service.AuthService;
import com.bank.fundtransfer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Customers", description = "Register and check login")
public class CustomerController {
    private final CustomerService customerService;
    private final AuthService authService;

    public CustomerController(CustomerService customerService, AuthService authService) {
        this.customerService = customerService;
        this.authService = authService;
    }

    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new customer (also opens the first account)")
    public RegistrationResponse register(@Valid @RequestBody RegisterRequest request) {
        Customer customer = customerService.register(request.name(), request.email(), request.phone(), request.pin());
        Account firstAccount = customerService.getAccounts(customer).get(0);
        return new RegistrationResponse(CustomerResponse.from(customer), AccountResponse.from(firstAccount));
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Check customer ID and PIN (later this will return a JWT token)")
    public CustomerResponse login(@Valid @RequestBody LoginRequest request) {
        return CustomerResponse.from(authService.authenticateCustomer(request.customerCode(), request.pin()));
    }
}