package com.bank.fundtransfer.repository;

import com.bank.fundtransfer.entity.Account;
import com.bank.fundtransfer.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySourceOrDestinationOrderByCreatedAtDesc(Account source, Account destination);
}