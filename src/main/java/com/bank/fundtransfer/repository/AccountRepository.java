package com.bank.fundtransfer.repository;

import com.bank.fundtransfer.entity.Account;
import com.bank.fundtransfer.entity.AccountRole;
import com.bank.fundtransfer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByOwner(Customer owner);

    Optional<Account> findFirstByRole(AccountRole role);
}