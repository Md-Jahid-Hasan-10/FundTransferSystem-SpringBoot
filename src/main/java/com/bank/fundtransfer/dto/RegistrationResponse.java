package com.bank.fundtransfer.dto;

public record RegistrationResponse(CustomerResponse customer, AccountResponse firstAccount) {
}