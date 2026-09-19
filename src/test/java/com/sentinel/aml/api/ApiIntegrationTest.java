package com.sentinel.aml.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ApiIntegrationTest {
    private static final String ADMIN = "test-admin";
    private static final String ADMIN_PASSWORD = "test-admin-password";
    private static final String ANALYST = "test-analyst";
    private static final String ANALYST_PASSWORD = "test-analyst-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publicStatusAndJsonAuthenticationErrorsAreConsistent() throws Exception {
        mockMvc.perform(get("/api/v1/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        mockMvc.perform(multipart("/api/v1/ingestion/customers/csv")
                        .file(csv("customers.csv"))
                        .with(httpBasic(ANALYST, ANALYST_PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void validationFailuresUseTheStandardErrorShape() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.transactionId").exists())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void onlyAdministratorsCanReadRuleConfiguration() throws Exception {
        mockMvc.perform(get("/api/v1/rules").with(httpBasic(ANALYST, ANALYST_PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));

        mockMvc.perform(get("/api/v1/rules").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thresholdEnabled").value(true))
                .andExpect(jsonPath("$.exchangeRatesToInr.INR").value(1));
    }

    @Test
    void administratorCanUpdateRuleConfigurationAtRuntime() throws Exception {
        String configuration = mockMvc.perform(get("/api/v1/rules").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(put("/api/v1/rules")
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configuration))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thresholdEnabled").value(true));
    }

    @Test
    @Transactional
    void importsDetectsMasksInvestigatesAndAudits() throws Exception {
        upload("customers");
        upload("accounts");
        upload("transactions");

        String alertJson = mockMvc.perform(get("/api/v1/alerts").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].riskScore").value(100))
                .andExpect(jsonPath("$[0].customer").value("K***"))
                .andExpect(jsonPath("$[0].accountId").value(org.hamcrest.Matchers.matchesPattern("\\*{3}\\d{4}")))
                .andReturn().getResponse().getContentAsString();
        List<JsonNode> alerts = objectMapper.readValue(alertJson, new TypeReference<>() {});
        long alertId = alerts.get(0).get("id").asLong();

        mockMvc.perform(get("/api/v1/alerts/{id}", alertId).with(httpBasic(ANALYST, ANALYST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Krishna Sharma"))
                .andExpect(jsonPath("$.transactionIds").isArray());

        String caseJson = mockMvc.perform(post("/api/v1/cases/from-alert/{id}", alertId)
                        .with(httpBasic(ANALYST, ANALYST_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Review opened\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        long caseId = objectMapper.readTree(caseJson).get("id").asLong();

        mockMvc.perform(patch("/api/v1/cases/{id}", caseId)
                        .with(httpBasic(ANALYST, ANALYST_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\",\"notes\":\"Complete\","
                                + "\"disposition\":\"Escalated to FIU\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(get("/api/v1/audit-events").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.entityType == 'ALERT' && @.action == 'STATUS_CHANGED')]").exists())
                .andExpect(jsonPath("$[?(@.entityType == 'CASE' && @.action == 'STATUS_CHANGED')]").exists());
    }

    private void upload(String kind) throws Exception {
        mockMvc.perform(multipart("/api/v1/ingestion/{kind}/csv", kind)
                        .file(csv(kind + ".csv"))
                        .with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    private MockMultipartFile csv(String name) throws Exception {
        return new MockMultipartFile("file", name, "text/csv",
                Files.readAllBytes(Path.of("data", "import", name)));
    }
}