package com.sentinel.aml.performance;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import com.sentinel.aml.account.Account;
import com.sentinel.aml.account.AccountRepository;
import com.sentinel.aml.customer.Customer;
import com.sentinel.aml.customer.CustomerRepository;
import com.sentinel.aml.transaction.TransactionIngestionService;
import com.sentinel.aml.transaction.TransactionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:performance;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "logging.level.com.sentinel.aml=WARN"
})
@ActiveProfiles("test")
class BulkPerformanceIT {
    @Autowired
    private TransactionIngestionService ingestionService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void processesTenThousandTransactionsWithinTwoMinutes() {
        Customer customer = new Customer("PERF_CUSTOMER");
        customer.update("Performance", "Test", null, null, null, "Test", "Test", "IN",
                BigDecimal.ZERO, "RETAIL", "VERIFIED", "LOW", false);
        customerRepository.saveAndFlush(customer);
        Account account = new Account("PERF_ACCOUNT");
        account.update(customer, "SAVINGS", "ACTIVE", "INR", "LOW", LocalDate.now(),
                BigDecimal.ZERO, "PERF", "SILVER");
        accountRepository.saveAndFlush(account);

        Instant timestamp = Instant.parse("2026-09-19T00:00:00Z");
        long startedAt = System.nanoTime();
        for (int index = 0; index < 10_000; index++) {
            ingestionService.ingest(new TransactionRequest("PERF_TXN_" + index, account.getExternalId(),
                    "CREDIT", new BigDecimal("1.11"), "INR", "Performance", "API",
                    timestamp.plusSeconds(index), "IN"));
        }
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        assertThat(elapsed).isLessThan(Duration.ofMinutes(2));
    }
}