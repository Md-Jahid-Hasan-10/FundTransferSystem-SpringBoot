package com.bank.fundtransfer.repository;

import com.bank.fundtransfer.entity.OtpRecord;
import com.bank.fundtransfer.entity.TransactionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRecordRepository extends JpaRepository<OtpRecord, Long> {
    Optional<OtpRecord> findByRequest(TransactionRequest request);
}