package com.sentinel.aml.transaction;

import java.math.BigDecimal;

public interface ActivitySummary {
    BigDecimal getTotalAmount();
    long getTransactionCount();
}