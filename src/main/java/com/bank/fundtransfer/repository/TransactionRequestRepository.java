package com.bank.fundtransfer.repository;

import com.bank.fundtransfer.entity.TransactionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRequestRepository extends JpaRepository<TransactionRequest, Long> {
    Optional<TransactionRequest> findByRequestCode(String requestCode);
}