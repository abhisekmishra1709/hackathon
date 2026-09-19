package com.sentinel.aml.detection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.sentinel.aml.account.Account;
import com.sentinel.aml.alert.AlertRepository;
import com.sentinel.aml.audit.AuditEventRepository;
import com.sentinel.aml.customer.Customer;
import com.sentinel.aml.transaction.Transaction;
import com.sentinel.aml.transaction.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DetectionEngineTest {
    private DetectionEngine engine;
    private Account account;
    private AmlRuleProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AmlRuleProperties();
        engine = new DetectionEngine(properties, mock(TransactionRepository.class), mock(AlertRepository.class),
            mock(AuditEventRepository.class));
        Customer customer = mock(Customer.class);
        account = mock(Account.class);
        when(account.getCustomer()).thenReturn(customer);
        when(account.getExternalId()).thenReturn("ACC_TEST");
    }

    @Test
    void flagsReportableTransaction() {
        Transaction transaction = transaction("T1", "CREDIT", "830000", "IN", "2026-09-19T10:00:00Z");
        assertThat(engine.threshold(transaction)).get().extracting(RuleMatch::code).isEqualTo("CTR_THRESHOLD");
    }

    @Test
    void flagsThreeJustBelowThresholdTransactions() {
        Transaction first = transaction("T1", "CREDIT", "800000", "IN", "2026-09-19T08:00:00Z");
        Transaction second = transaction("T2", "CREDIT", "800000", "IN", "2026-09-19T09:00:00Z");
        Transaction third = transaction("T3", "CREDIT", "800000", "IN", "2026-09-19T10:00:00Z");

        assertThat(engine.structuring(third, List.of(first, second, third)))
                .get().extracting(RuleMatch::code).isEqualTo("STRUCTURING");
    }

    @Test
    void flagsRapidMovementOfDeposit() {
        Transaction deposit = transaction("T1", "CREDIT", "1000000", "IN", "2026-09-18T10:00:00Z");
        Transaction firstWithdrawal = transaction("T2", "DEBIT", "400000", "AE", "2026-09-19T09:00:00Z");
        Transaction secondWithdrawal = transaction("T3", "DEBIT", "400000", "AE", "2026-09-19T10:00:00Z");

        assertThat(engine.rapidMovement(secondWithdrawal, List.of(deposit, firstWithdrawal, secondWithdrawal)))
                .get().extracting(RuleMatch::code).isEqualTo("RAPID_MOVEMENT");
    }

    @Test
    void flagsHighRiskJurisdictionRegardlessOfAmount() {
        Transaction transaction = transaction("T1", "DEBIT", "1", "IR", "2026-09-19T10:00:00Z");
        assertThat(engine.highRisk(transaction)).get().extracting(RuleMatch::score).isEqualTo(95);
    }

    @Test
    void flagsConfiguredHighRiskCounterparty() {
        properties.setHighRiskCounterparties(java.util.Set.of("Blocked Entity"));
        Transaction transaction = new Transaction("T1", account, "DEBIT", BigDecimal.ONE, "INR",
                BigDecimal.ONE, "Blocked Entity", "WIRE", "IN", Instant.parse("2026-09-19T10:00:00Z"));

        assertThat(engine.highRisk(transaction)).isPresent();
    }

    @Test
    void disabledRuleDoesNotMatch() {
        properties.setThresholdEnabled(false);
        Transaction transaction = transaction("T1", "CREDIT", "900000", "IN", "2026-09-19T10:00:00Z");

        assertThat(engine.threshold(transaction)).isEmpty();
    }

    @Test
    void flagsBehaviorAboveThreeTimesNinetyDayAverage() {
        Transaction baseline = transaction("T1", "CREDIT", "90000", "IN", "2026-09-01T10:00:00Z");
        Transaction current = transaction("T2", "CREDIT", "4000", "IN", "2026-09-19T10:00:00Z");

        assertThat(engine.behavioralDeviation(current, List.of(baseline, current)))
                .get().extracting(RuleMatch::code).isEqualTo("BEHAVIORAL_DEVIATION");
    }

    @Test
    void flagsRepeatedRoundNumberTransactions() {
        Transaction first = transaction("T1", "CREDIT", "100000", "IN", "2026-09-19T09:00:00Z");
        Transaction second = transaction("T2", "CREDIT", "200000", "IN", "2026-09-19T10:00:00Z");

        assertThat(engine.roundNumber(second, List.of(first, second)))
                .get().extracting(RuleMatch::code).isEqualTo("ROUND_NUMBER");
    }

    private Transaction transaction(String id, String type, String amount, String jurisdiction, String timestamp) {
        BigDecimal value = new BigDecimal(amount);
        return new Transaction(id, account, type, value, "INR", value, "Counterparty", "WIRE",
                jurisdiction, Instant.parse(timestamp));
    }
}