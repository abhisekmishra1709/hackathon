package com.sentinel.aml.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sentinel.aml.account.AccountRepository;
import com.sentinel.aml.customer.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:csv-import;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@Transactional
class CsvImportServiceTest {
    @Autowired
    private CsvImportService csvImportService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void clearData() {
        accountRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    void importsSuppliedCustomersAndLinkedAccounts() throws IOException {
        ImportResult customers = csvImportService.importCustomers(file("customers.csv"));
        ImportResult accounts = csvImportService.importAccounts(file("accounts.csv"));

        assertThat(customers.created()).isEqualTo(2);
        assertThat(accounts.created()).isEqualTo(2);
        assertThat(accounts.errors()).isEmpty();
        assertThat(accountRepository.findByExternalId("ACC_000001").orElseThrow()
                .getCustomer().getExternalId()).isEqualTo("CUST_00001");
    }

    @Test
    void rejectsAccountForUnknownCustomer() {
        String csv = "account_id,customer_id,account_type,account_status,currency,open_date,current_balance,branch_code,account_tier\n"
                + "ACC_BAD,CUST_MISSING,SAVINGS,ACTIVE,INR,2026-01-01,1000,BR001,SILVER\n";
        MockMultipartFile file = new MockMultipartFile("file", "accounts.csv", "text/csv", csv.getBytes());

        ImportResult result = csvImportService.importAccounts(file);

        assertThat(result.created()).isZero();
        assertThat(result.errors()).singleElement().asString().contains("Unknown customer_id CUST_MISSING");
    }

    private MockMultipartFile file(String name) throws IOException {
        Path path = Path.of("data", "import", name);
        return new MockMultipartFile("file", name, "text/csv", Files.newInputStream(path));
    }
}