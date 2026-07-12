package integration.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import moo.interview.teya.Application;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LedgerApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void endToEnd_flow_usesEndpointResponsesForAssertions() throws Exception {
        String accountNumber = createAccountAndGetNumber();

        ResponseEntity<String> deposit = postJson(
                path("/accounts/" + accountNumber + "/transactions/deposit"),
                "{\"amount\":500.00,\"description\":\"Initial funding\"}"
        );
        assertEquals(HttpStatus.CREATED, deposit.getStatusCode());

        ResponseEntity<String> invalidWithdrawal = postJson(
                path("/accounts/" + accountNumber + "/transactions/withdraw"),
                "{\"amount\":5000.00,\"description\":\"Too much\"}"
        );
        assertEquals(HttpStatus.CONFLICT, invalidWithdrawal.getStatusCode());

        JsonNode invalidNode = objectMapper.readTree(invalidWithdrawal.getBody());
        assertEquals("INSUFFICIENT_BALANCE", invalidNode.get("errorCode").asText());

        ResponseEntity<String> withdrawal = postJson(
                path("/accounts/" + accountNumber + "/transactions/withdraw"),
                "{\"amount\":200.00,\"description\":\"Cash out\"}"
        );
        assertEquals(HttpStatus.ACCEPTED, withdrawal.getStatusCode());

        // Withdrawal processing is async; poll balance until queue worker applies it.
        JsonNode finalBalanceNode = awaitBalance(accountNumber, new BigDecimal("300.00"));
        assertEquals("GBP", finalBalanceNode.get("currency").asText());
        assertNotNull(finalBalanceNode.get("lastUpdatedAtInUTC").asText());

        ResponseEntity<String> history = restTemplate.getForEntity(
                path("/accounts/" + accountNumber + "/transactions?pageSize=10"),
                String.class
        );
        assertEquals(HttpStatus.OK, history.getStatusCode());

        JsonNode historyNode = objectMapper.readTree(history.getBody());
        assertTrue(historyNode.get("transactions").size() >= 2);
    }

    @Test
    void history_cursorPagination_returnsNextCursorAndNextPage() throws Exception {
        String accountNumber = createAccountAndGetNumber();

        postJson(path("/accounts/" + accountNumber + "/transactions/deposit"), "{\"amount\":10.00,\"description\":\"d1\"}");
        postJson(path("/accounts/" + accountNumber + "/transactions/deposit"), "{\"amount\":20.00,\"description\":\"d2\"}");
        postJson(path("/accounts/" + accountNumber + "/transactions/deposit"), "{\"amount\":30.00,\"description\":\"d3\"}");

        ResponseEntity<String> page1 = restTemplate.getForEntity(
                path("/accounts/" + accountNumber + "/transactions?pageSize=2"),
                String.class
        );
        assertEquals(HttpStatus.OK, page1.getStatusCode());

        JsonNode page1Node = objectMapper.readTree(page1.getBody());
        assertEquals(2, page1Node.get("transactions").size());
        assertTrue(page1Node.get("pageInfo").get("hasNextPage").asBoolean());
        String nextCursor = page1Node.get("pageInfo").get("nextCursor").asText();
        assertNotNull(nextCursor);
        assertFalse(nextCursor.isBlank());

        ResponseEntity<String> page2 = restTemplate.getForEntity(
                path("/accounts/" + accountNumber + "/transactions?pageSize=2&cursor=" + nextCursor),
                String.class
        );
        assertEquals(HttpStatus.OK, page2.getStatusCode());

        JsonNode page2Node = objectMapper.readTree(page2.getBody());
        assertTrue(page2Node.get("transactions").size() >= 1);
    }

    @Test
    void history_invalidPageSize_returnsValidationError() throws Exception {
        String accountNumber = createAccountAndGetNumber();

        ResponseEntity<String> invalid = restTemplate.getForEntity(
                path("/accounts/" + accountNumber + "/transactions?pageSize=0"),
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, invalid.getStatusCode());
        JsonNode node = objectMapper.readTree(invalid.getBody());
        assertEquals("VALIDATION_ERROR", node.get("errorCode").asText());
    }

    private String createAccountAndGetNumber() throws Exception {
        ResponseEntity<String> createAccount = postJson(path("/accounts"), "{}");
        assertEquals(HttpStatus.CREATED, createAccount.getStatusCode());

        JsonNode node = objectMapper.readTree(createAccount.getBody());
        String accountNumber = node.get("accountNumber").asText();
        assertTrue(accountNumber.startsWith("ACC-"));
        return accountNumber;
    }

    private JsonNode awaitBalance(String accountNumber, BigDecimal expectedDisplayBalance) throws Exception {
        Instant deadline = Instant.now().plus(6, ChronoUnit.SECONDS);
        while (Instant.now().isBefore(deadline)) {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    path("/accounts/" + accountNumber + "/balance"),
                    String.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode node = objectMapper.readTree(response.getBody());
                BigDecimal balance = new BigDecimal(node.get("currentBalance").asText());
                if (balance.compareTo(expectedDisplayBalance) == 0) {
                    return node;
                }
            }
            Thread.sleep(200);
        }
        fail("Balance did not reach expected value within timeout");
        return null;
    }

    private ResponseEntity<String> postJson(String url, String jsonBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(jsonBody, headers), String.class);
    }

    private String path(String relative) {
        return "http://localhost:" + port + "/api" + relative;
    }
}

