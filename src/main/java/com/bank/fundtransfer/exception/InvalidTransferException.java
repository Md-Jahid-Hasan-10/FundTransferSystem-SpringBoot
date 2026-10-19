package com.bank.fundtransfer.exception;

public class InvalidTransferException extends BankException {
    public InvalidTransferException(String message) {
        super(message);
    }
}