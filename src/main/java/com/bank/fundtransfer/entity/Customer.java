package com.bank.fundtransfer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_code", nullable = false, unique = true, length = 20)
    private String customerCode;      // readable ID such as CUST1001

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, unique = true, length = 30)
    private String phone;

    @Column(name = "pin_hash", nullable = false, length = 100)
    private String pinHash;           // never store the plain PIN

    protected Customer() {
        // required by JPA
    }

    public Customer(String customerCode, String name, String email, String phone, String pinHash) {
        this.customerCode = customerCode;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.pinHash = pinHash;
    }

    public Long getId() { return id; }
    public String getCustomerCode() { return customerCode; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPinHash() { return pinHash; }
}