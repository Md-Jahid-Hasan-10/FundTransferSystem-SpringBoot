package com.bank.fundtransfer.entity;

public enum RequestStatus {
    PENDING_OTP,
    PENDING_CONFIRMATION,
    SUCCESS,
    FAILED,
    CANCELLED
}