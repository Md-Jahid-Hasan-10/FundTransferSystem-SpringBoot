package com.bank.fundtransfer.controller;

import com.bank.fundtransfer.config.BankProperties;
import com.bank.fundtransfer.dto.AmountRequest;
import com.bank.fundtransfer.dto.AuthHeaders;
import com.bank.fundtransfer.dto.LimitsResponse;
import com.bank.fundtransfer.dto.MessageResponse;
import com.bank.fundtransfer.dto.OtpRequest;
import com.bank.fundtransfer.dto.TransactionRequestResponse;
import com.bank.fundtransfer.dto.TransactionResponse;
import com.bank.fundtransfer.dto.TransferRequest;
import com.bank.fundtransfer.dto.WithdrawalRequest;
import com.bank.fundtransfer.entity.Customer;
import com.bank.fundtransfer.entity.TransactionRequest;
import com.bank.fundtransfer.service.AuthService;
import com.bank.fundtransfer.service.FundTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Money operations. Withdrawals and transfers take three calls:
 * 1) create the request (an OTP is sent), 2) verify the OTP, 3) confirm (or cancel).
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Transactions", description = "Deposit, withdraw, transfer, OTP and confirmation")
public class TransactionController {
    private final AuthService authService;
    private final FundTransferService service;
    private final BankProperties properties;

    public TransactionController(AuthService authService, FundTransferService service, BankProperties properties) {
        this.authService = authService;
        this.service = service;
        this.properties = properties;
    }

    @PostMapping("/accounts/{accountNumber}/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Deposit money into one of my accounts (no OTP needed)")
    public TransactionResponse deposit(
            @RequestHeader(AuthHeaders.CUSTOMER_CODE) String customerCode,
            @RequestHeader(AuthHeaders.CUSTOMER_PIN) String pin,
            @PathVariable String accountNumber,
            @Valid @RequestBody AmountRequest request) {
        Customer customer = authService.authenticateCustomer(customerCode, pin);
        return TransactionResponse.from(service.deposit(customer, accountNumber, request.amount()));
    }

    @PostMapping("/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Step 1: request a withdrawal. An OTP is sent to the customer")
    public TransactionRequestResponse requestWithdrawal(
            @RequestHeader(AuthHeaders.CUSTOMER_CODE) String customerCode,
            @RequestHeader(AuthHeaders.CUSTOMER_PIN) String pin,
            @Valid @RequestBody WithdrawalRequest request) {
        Customer customer = authService.authenticateCustomer(customerCode, pin);
        TransactionRequest created = service.requestWithdrawal(customer, request.accountNumber(), request.amount());
        return TransactionRequestResponse.from(created, "An OTP was sent. Verify it to continue.");
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Step 1: request a transfer. The response shows the fee and total. An OTP is sent")
    public TransactionRequestResponse requestTransfer(
            @RequestHeader(AuthHeaders.CUSTOMER_CODE) String customerCode,
            @RequestHeader(AuthHeaders.CUSTOMER_PIN) String pin,
            @Valid @RequestBody TransferRequest request) {
        Customer customer = authService.authenticateCustomer(customerCode, pin);
        TransactionRequest created = service.requestTransfer(customer, request.fromAccount(),
                request.toAccount(), request.amount());
        return TransactionRequestResponse.from(created, "An OTP was sent. Verify it to continue.");
    }

    @PostMapping("/requests/{requestCode}/verify-otp")
    @Operation(summary = "Step 2: verify the OTP")
    public TransactionRequestResponse verifyOtp(
            @RequestHeader(AuthHeaders.CUSTOMER_CODE) String customerCode,
            @RequestHeader(AuthHeaders.CUSTOMER_PIN) String pin,
            @PathVariable String requestCode,
            @Valid @RequestBody OtpRequest body) {
        Customer customer = authService.authenticateCustomer(customerCode, pin);
        service.verifyOtp(customer, requestCode, body.otp());
        TransactionRequest request = service.getRequest(customer, requestCode);
        return TransactionRequestResponse.from(request,
                "OTP verified. Review the amount, fee and total, then confirm or cancel.");
    }

    @PostMapping("/requests/{requestCode}/confirm")
    @Operation(summary = "Step 3: confirm. This moves the money")
    public TransactionResponse confirm(
            @RequestHeader(AuthHeaders.CUSTOMER_CODE) String customerCode,
            @RequestHeader(AuthHeaders.CUSTOMER_PIN) String pin,
            @PathVariable String requestCode) {
        Customer customer = authService.authenticateCustomer(customerCode, pin);
        return TransactionResponse.from(service.confirm(customer, requestCode));
    }

    @PostMapping("/requests/{requestCode}/cancel")
    @Operation(summary = "Cancel a request that has not been confirmed. No money moves")
    public MessageResponse cancel(
            @RequestHeader(AuthHeaders.CUSTOMER_CODE) String customerCode,
            @RequestHeader(AuthHeaders.CUSTOMER_PIN) String pin,
            @PathVariable String requestCode) {
        Customer customer = authService.authenticateCustomer(customerCode, pin);
        service.cancel(customer, requestCode);
        return new MessageResponse("Request " + requestCode + " was cancelled. No money was moved.");
    }

    @GetMapping("/me/limits")
    @Operation(summary = "My limits and how much of today's limit is used")
    public LimitsResponse limits(
            @RequestHeader(AuthHeaders.CUSTOMER_CODE) String customerCode,
            @RequestHeader(AuthHeaders.CUSTOMER_PIN) String pin) {
        Customer customer = authService.authenticateCustomer(customerCode, pin);
        BigDecimal used = service.getTodaysOutgoingTotal(customer);
        BigDecimal remaining = properties.dailyLimit().subtract(used).max(BigDecimal.ZERO);
        return new LimitsResponse(properties.perTransactionLimit(), properties.dailyLimit(),
                used, remaining, properties.minimumBalance());
    }
}