package com.bank.fundtransfer.exception;

public class InvalidAmountException extends BankException {
    public InvalidAmountException(String message) {
        super(message);
    }
}