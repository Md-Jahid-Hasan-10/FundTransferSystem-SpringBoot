package com.bank.fundtransfer.exception;

public class OtpExpiredException extends BankException {
    public OtpExpiredException(String message) {
        super(message);
    }
}