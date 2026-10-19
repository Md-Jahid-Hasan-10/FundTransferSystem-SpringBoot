package com.bank.fundtransfer.exception;

public class InsufficientFundsException extends BankException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}