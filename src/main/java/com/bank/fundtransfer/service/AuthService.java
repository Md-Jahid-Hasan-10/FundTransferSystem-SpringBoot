package com.bank.fundtransfer.service;

import com.bank.fundtransfer.entity.Customer;
import org.springframework.stereotype.Service;

/**
 * Checks who is calling. For now the caller sends their credentials in headers with every request.
 * This is a temporary, simple scheme. Phase 6 replaces it with login + JWT tokens, and only this
 * class and the headers will need to change.
 */
@Service
public class AuthService {
    private final CustomerService customerService;
    private final AdminService adminService;

    public AuthService(CustomerService customerService, AdminService adminService) {
        this.customerService = customerService;
        this.adminService = adminService;
    }

    public Customer authenticateCustomer(String customerCode, String pin) {
        return customerService.login(customerCode, pin);
    }

    public void authenticateAdmin(String username, String password) {
        adminService.login(username, password);
    }
}