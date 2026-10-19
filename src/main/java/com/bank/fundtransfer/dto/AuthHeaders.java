package com.bank.fundtransfer.dto;

/** Names of the temporary credential headers (replaced by JWT in Phase 6). */
public final class AuthHeaders {
    public static final String CUSTOMER_CODE = "X-Customer-Code";
    public static final String CUSTOMER_PIN = "X-Customer-Pin";
    public static final String ADMIN_USERNAME = "X-Admin-Username";
    public static final String ADMIN_PASSWORD = "X-Admin-Password";

    private AuthHeaders() {
    }
}