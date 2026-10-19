package com.bank.fundtransfer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Email is required") @Email(message = "Email is not valid") String email,
        @NotBlank(message = "Phone is required") String phone,
        @NotBlank(message = "PIN is required") @Size(min = 4, max = 20, message = "PIN must be 4 to 20 characters") String pin) {
}