package com.bank.fundtransfer.notification;

import org.springframework.stereotype.Component;

@Component
public class ConsoleNotification implements NotificationChannel {

    @Override
    public void send(String recipient, String message) {
        System.out.println("[NOTIFICATION -> " + recipient + "] " + message);
    }
}