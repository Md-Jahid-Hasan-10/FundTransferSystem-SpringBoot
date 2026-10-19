package com.bank.fundtransfer.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Customer code is required") String customerCode,
        @NotBlank(message = "PIN is required") String pin) {
}