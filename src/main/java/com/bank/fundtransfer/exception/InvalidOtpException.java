package com.bank.fundtransfer.exception;

public class InvalidOtpException extends BankException {
    private final int remainingAttempts;

    public InvalidOtpException(String message, int remainingAttempts) {
        super(message);
        this.remainingAttempts = remainingAttempts;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }
}