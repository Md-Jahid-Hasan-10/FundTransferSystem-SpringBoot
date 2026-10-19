package com.bank.fundtransfer.exception;

public class BankException extends RuntimeException {
    public BankException(String message) {
        super(message);
    }
}