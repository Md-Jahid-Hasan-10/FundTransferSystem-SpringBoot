package com.bank.fundtransfer.exception;

public class TransactionLimitExceededException extends BankException {
    public TransactionLimitExceededException(String message) {
        super(message);
    }
}