package com.sentinel.aml.transaction;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransactionRequest(
        @NotBlank String transactionId,
        @NotBlank String accountId,
        @NotBlank String transactionType,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String currency,
        String counterparty,
        @NotBlank String channel,
        @NotNull Instant timestamp,
        @NotBlank String jurisdiction) {
}