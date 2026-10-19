package com.bank.fundtransfer.controller;

import com.bank.fundtransfer.dto.AccountResponse;
import com.bank.fundtransfer.dto.AuthHeaders;
import com.bank.fundtransfer.dto.CustomerResponse;
import com.bank.fundtransfer.dto.FeeReportResponse;
import com.bank.fundtransfer.dto.MessageResponse;
import com.bank.fundtransfer.dto.TransactionResponse;
import com.bank.fundtransfer.entity.Account;
import com.bank.fundtransfer.service.AdminService;
import com.bank.fundtransfer.service.AuthService;
import com.bank.fundtransfer.service.FundTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Bank staff only")
public class AdminController {
    private final AuthService authService;
    private final AdminService adminService;
    private final FundTransferService transferService;

    public AdminController(AuthService authService, AdminService adminService,
                           FundTransferService transferService) {
        this.authService = authService;
        this.adminService = adminService;
        this.transferService = transferService;
    }

    @GetMapping("/customers")
    @Operation(summary = "List all customers")
    public List<CustomerResponse> customers(
            @RequestHeader(AuthHeaders.ADMIN_USERNAME) String username,
            @RequestHeader(AuthHeaders.ADMIN_PASSWORD) String password) {
        authService.authenticateAdmin(username, password);
        return adminService.getAllCustomers().stream().map(CustomerResponse::from).toList();
    }

    @GetMapping("/accounts")
    @Operation(summary = "List all accounts, including the bank fee account")
    public List<AccountResponse> accounts(
            @RequestHeader(AuthHeaders.ADMIN_USERNAME) String username,
            @RequestHeader(AuthHeaders.ADMIN_PASSWORD) String password) {
        authService.authenticateAdmin(username, password);
        return adminService.getAllAccounts().stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/transactions")
    @Operation(summary = "List every transaction, including failed and cancelled ones")
    public List<TransactionResponse> transactions(
            @RequestHeader(AuthHeaders.ADMIN_USERNAME) String username,
            @RequestHeader(AuthHeaders.ADMIN_PASSWORD) String password) {
        authService.authenticateAdmin(username, password);
        return adminService.getAllTransactions().stream().map(TransactionResponse::from).toList();
    }

    @GetMapping("/fees")
    @Operation(summary = "Bank fee report: total fees collected and the transfers that paid a fee")
    public FeeReportResponse fees(
            @RequestHeader(AuthHeaders.ADMIN_USERNAME) String username,
            @RequestHeader(AuthHeaders.ADMIN_PASSWORD) String password) {
        authService.authenticateAdmin(username, password);
        Account feeAccount = transferService.getBankFeeAccount();
        List<TransactionResponse> transfers = transferService.getFeeTransactions().stream()
                .map(TransactionResponse::from).toList();
        return new FeeReportResponse(feeAccount.getAccountNumber(), feeAccount.getBalance(),
                transfers.size(), transfers);
    }

    @PatchMapping("/accounts/{accountNumber}/freeze")
    @Operation(summary = "Freeze an account. The customer is notified")
    public MessageResponse freeze(
            @RequestHeader(AuthHeaders.ADMIN_USERNAME) String username,
            @RequestHeader(AuthHeaders.ADMIN_PASSWORD) String password,
            @PathVariable String accountNumber) {
        authService.authenticateAdmin(username, password);
        adminService.freezeAccount(accountNumber);
        return new MessageResponse("Account " + accountNumber + " was frozen.");
    }

    @PatchMapping("/accounts/{accountNumber}/unfreeze")
    @Operation(summary = "Unfreeze an account. The customer is notified")
    public MessageResponse unfreeze(
            @RequestHeader(AuthHeaders.ADMIN_USERNAME) String username,
            @RequestHeader(AuthHeaders.ADMIN_PASSWORD) String password,
            @PathVariable String accountNumber) {
        authService.authenticateAdmin(username, password);
        adminService.unfreezeAccount(accountNumber);
        return new MessageResponse("Account " + accountNumber + " was unfrozen.");
    }
}