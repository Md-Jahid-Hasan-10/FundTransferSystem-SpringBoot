package com.bank.fundtransfer.repository;

import com.bank.fundtransfer.entity.Account;
import com.bank.fundtransfer.entity.Customer;
import com.bank.fundtransfer.entity.Transaction;
import com.bank.fundtransfer.entity.TransactionStatus;
import com.bank.fundtransfer.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findBySourceOrDestinationOrderByCreatedAtDesc(Account source, Account destination);

    /** Successful transfers that earned the bank a fee (used by the bank fee report). */
    List<Transaction> findByTypeAndStatusAndFeeGreaterThanOrderByCreatedAtDesc(
            TransactionType type, TransactionStatus status, BigDecimal fee);

    /**
     * Amounts a customer sent out in a time window: withdrawals plus transfers to OTHER customers.
     * Transfers between the customer's own accounts are not included.
     */
    @Query("""
            select t.amount
            from Transaction t
            join t.source s
            left join t.destination d
            where s.owner = :owner
              and t.status = :status
              and t.createdAt >= :from
              and t.createdAt < :to
              and (t.type = :withdrawal or (t.type = :transfer and d.owner <> s.owner))
            """)
    List<BigDecimal> findOutgoingAmounts(@Param("owner") Customer owner,
                                         @Param("status") TransactionStatus status,
                                         @Param("withdrawal") TransactionType withdrawal,
                                         @Param("transfer") TransactionType transfer,
                                         @Param("from") Instant from,
                                         @Param("to") Instant to);

    default List<BigDecimal> outgoingAmountsBetween(Customer owner, Instant from, Instant to) {
        return findOutgoingAmounts(owner, TransactionStatus.SUCCESS,
                TransactionType.WITHDRAWAL, TransactionType.TRANSFER, from, to);
    }
}