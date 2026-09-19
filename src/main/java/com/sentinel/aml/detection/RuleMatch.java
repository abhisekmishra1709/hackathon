package com.sentinel.aml.detection;

import java.util.List;

import com.sentinel.aml.transaction.Transaction;

public record RuleMatch(String code, int score, String explanation, List<Transaction> evidence) {
}