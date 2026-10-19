package com.bank.fundtransfer.exception;

public class AuthenticationFailedException extends BankException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}