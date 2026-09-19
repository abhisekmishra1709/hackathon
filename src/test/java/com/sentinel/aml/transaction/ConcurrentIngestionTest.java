package com.sentinel.aml.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.sentinel.aml.account.Account;
import com.sentinel.aml.account.AccountRepository;
import com.sentinel.aml.customer.Customer;
import com.sentinel.aml.customer.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConcurrentIngestionTest {
    @Autowired
    private TransactionIngestionService ingestionService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void concurrentDuplicateTransactionIsStoredOnce() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        Customer customer = new Customer("CONCURRENT_CUSTOMER_" + suffix);
        customer.update("Test", "Customer", null, null, null, "Test", "Test", "IN",
                BigDecimal.ZERO, "RETAIL", "VERIFIED", "LOW", false);
        customerRepository.saveAndFlush(customer);
        Account account = new Account("CONCURRENT_ACCOUNT_" + suffix);
        account.update(customer, "SAVINGS", "ACTIVE", "INR", "LOW", java.time.LocalDate.now(),
                BigDecimal.ZERO, "TEST", "SILVER");
        accountRepository.saveAndFlush(account);

        TransactionRequest request = new TransactionRequest("CONCURRENT_TXN_" + suffix, account.getExternalId(),
                "CREDIT", new BigDecimal("1000"), "INR", "Test", "API", Instant.now(), "IN");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<TransactionResult> first = executor.submit(() -> { start.await(); return ingestionService.ingest(request); });
            Future<TransactionResult> second = executor.submit(() -> { start.await(); return ingestionService.ingest(request); });
            start.countDown();

            List<Boolean> created = List.of(first.get().created(), second.get().created());
            assertThat(created).containsExactlyInAnyOrder(true, false);
            assertThat(transactionRepository.findByExternalId(request.transactionId())).isPresent();
        } finally {
            executor.shutdownNow();
        }
    }
}