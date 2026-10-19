package com.bank.fundtransfer.dto;

import com.bank.fundtransfer.entity.Customer;

/** Never includes the PIN or its hash. */
public record CustomerResponse(String customerCode, String name, String email, String phone) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.getCustomerCode(), customer.getName(),
                customer.getEmail(), customer.getPhone());
    }
}