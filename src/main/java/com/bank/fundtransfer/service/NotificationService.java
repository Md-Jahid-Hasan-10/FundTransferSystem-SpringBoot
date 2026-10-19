package com.bank.fundtransfer.service;

import com.bank.fundtransfer.config.BankProperties;
import com.bank.fundtransfer.entity.Account;
import com.bank.fundtransfer.entity.Customer;
import com.bank.fundtransfer.entity.Transaction;
import com.bank.fundtransfer.entity.TransactionRequest;
import com.bank.fundtransfer.notification.NotificationChannel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Builds the messages. The NotificationChannel decides how they are delivered. */
@Service
public class NotificationService {
    private final NotificationChannel channel;
    private final BankProperties properties;

    public NotificationService(NotificationChannel channel, BankProperties properties) {
        this.channel = channel;
        this.properties = properties;
    }

    public void sendOtp(Customer customer, TransactionRequest request, String otp) {
        send(customer, "Your OTP for request " + request.getRequestCode() + " is " + otp
                + ". It is valid for " + properties.otp().expirySeconds() + " seconds. Never share it.");
    }

    public void notifySuccess(Transaction tx) {
        switch (tx.getType()) {
            case DEPOSIT -> send(tx.getDestination().getOwner(),
                    "Deposit of " + money(tx.getAmount()) + " into " + tx.getDestination().getAccountNumber()
                            + " successful. Balance: " + money(tx.getDestination().getBalance()) + ".");
            case WITHDRAWAL -> send(tx.getSource().getOwner(),
                    "Withdrawal of " + money(tx.getAmount()) + " from " + tx.getSource().getAccountNumber()
                            + " successful. Balance: " + money(tx.getSource().getBalance()) + ".");
            case TRANSFER -> {
                send(tx.getSource().getOwner(),
                        "Transfer of " + money(tx.getAmount()) + " to " + tx.getDestination().getAccountNumber()
                                + " successful. Fee: " + money(tx.getFee())
                                + ". Balance: " + money(tx.getSource().getBalance()) + ".");
                if (!tx.getSource().hasSameOwnerAs(tx.getDestination())) {
                    send(tx.getDestination().getOwner(),
                            "You received " + money(tx.getAmount()) + " from " + tx.getSource().getAccountNumber()
                                    + ". Balance: " + money(tx.getDestination().getBalance()) + ".");
                }
            }
        }
    }

    public void notifyFailure(TransactionRequest request, String reason) {
        send(request.getCustomer(), "Your " + request.getType() + " request " + request.getRequestCode()
                + " failed. Reason: " + reason + " No money was moved.");
    }

    public void notifyCancelled(TransactionRequest request) {
        send(request.getCustomer(), "Your " + request.getType() + " request " + request.getRequestCode()
                + " was cancelled. No money was moved.");
    }

    public void notifyAccountStatusChanged(Account account, boolean frozen) {
        String text = frozen
                ? "frozen. You cannot use it until the bank unfreezes it."
                : "unfrozen. You can use it again.";
        send(account.getOwner(), "Your account " + account.getAccountNumber() + " has been " + text);
    }

    private String money(BigDecimal amount) {
        return properties.currencySymbol() + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void send(Customer customer, String message) {
        channel.send(customer.getName() + " (" + customer.getPhone() + ")", message);
    }
}