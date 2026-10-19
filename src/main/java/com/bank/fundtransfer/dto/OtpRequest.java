package com.bank.fundtransfer.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpRequest(@NotBlank(message = "OTP is required") String otp) {
}