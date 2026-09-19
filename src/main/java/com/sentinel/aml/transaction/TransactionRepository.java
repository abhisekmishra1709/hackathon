package com.sentinel.aml.transaction;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.sentinel.aml.account.Account;
import com.sentinel.aml.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByExternalId(String externalId);
    List<Transaction> findByAccountAndOccurredAtBetweenOrderByOccurredAt(Account account, Instant from, Instant to);
    List<Transaction> findByAccountCustomerAndOccurredAtBetweenOrderByOccurredAt(
            Customer customer, Instant from, Instant to);

        @Query("""
            select coalesce(sum(transaction.amountInr), 0) as totalAmount,
               count(transaction) as transactionCount
            from Transaction transaction
            where transaction.account.customer = :customer
              and transaction.occurredAt >= :from
              and transaction.occurredAt < :to
            """)
        ActivitySummary summarizeCustomerActivity(@Param("customer") Customer customer,
            @Param("from") Instant from, @Param("to") Instant to);
}