package com.bank.fundtransfer.repository;

import com.bank.fundtransfer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCustomerCode(String customerCode);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}