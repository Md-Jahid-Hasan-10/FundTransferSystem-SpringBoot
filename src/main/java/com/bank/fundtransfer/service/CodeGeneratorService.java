package com.bank.fundtransfer.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

/** Creates readable IDs (CUST1001, ACC1001, REQ1001, TXN1001) from PostgreSQL sequences. */
@Service
public class CodeGeneratorService {

    @PersistenceContext
    private EntityManager entityManager;

    public String nextCustomerCode() {
        return "CUST" + next("customer_code_seq");
    }

    public String nextAccountNumber() {
        return "ACC" + next("account_number_seq");
    }

    public String nextRequestCode() {
        return "REQ" + next("request_code_seq");
    }

    public String nextTransactionCode() {
        return "TXN" + next("transaction_code_seq");
    }

    private long next(String sequenceName) {
        // sequenceName only ever comes from the constants above, never from user input
        Number value = (Number) entityManager
                .createNativeQuery("select nextval('" + sequenceName + "')")
                .getSingleResult();
        return value.longValue();
    }
}