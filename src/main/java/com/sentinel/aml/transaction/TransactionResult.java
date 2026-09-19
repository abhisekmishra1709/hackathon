package com.sentinel.aml.transaction;

public record TransactionResult(String transactionId, Long alertId, long evaluationTimeMs, boolean created) {
}