package com.sentinel.aml.alert;

import java.math.BigDecimal;
import java.time.Instant;

import com.sentinel.aml.transaction.Transaction;

public record TransactionTimelineEntry(String externalId, String transactionType, BigDecimal amount,
        String currency, BigDecimal amountInr, String counterparty, String channel, String jurisdiction,
        Instant occurredAt, boolean evidence) {

    static TransactionTimelineEntry from(Transaction transaction, boolean evidence) {
        return new TransactionTimelineEntry(transaction.getExternalId(), transaction.getTransactionType(),
                transaction.getAmount(), transaction.getCurrency(), transaction.getAmountInr(),
                transaction.getCounterparty(), transaction.getChannel(), transaction.getJurisdiction(),
                transaction.getOccurredAt(), evidence);
    }
}
