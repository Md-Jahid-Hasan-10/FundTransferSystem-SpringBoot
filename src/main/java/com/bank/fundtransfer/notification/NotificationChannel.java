package com.bank.fundtransfer.notification;

public interface NotificationChannel {
    void send(String recipient, String message);
}