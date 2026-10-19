package com.bank.fundtransfer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the REST API over simulated HTTP calls (no real server needed).
 * Each test rolls back at the end. The OTP is fixed to 111111 in application-test.yml.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ApiFlowTest {
    private static final String OTP = "111111";
    private static final String[] ADMIN = {"X-Admin-Username", "admin", "X-Admin-Password", "admin123"};

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // ---------- small helpers ----------
    private ResultActions send(MockHttpServletRequestBuilder builder, String... headers) throws Exception {
        for (int i = 0; i < headers.length; i += 2) {
            builder.header(headers[i], headers[i + 1]);
        }
        return mockMvc.perform(builder);
    }

    private ResultActions post(String url, String json, String... headers) throws Exception {
        return send(MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON).content(json), headers);
    }

    private ResultActions get(String url, String... headers) throws Exception {
        return send(MockMvcRequestBuilders.get(url), headers);
    }

    private ResultActions patch(String url, String... headers) throws Exception {
        return send(MockMvcRequestBuilders.patch(url), headers);
    }

    private static String body(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static String extract(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        assertTrue(matcher.find(), "field " + field + " not found in " + json);
        return matcher.group(1);
    }

    private static String[] login(String customerCode, String pin) {
        return new String[]{"X-Customer-Code", customerCode, "X-Customer-Pin", pin};
    }

    /** Registers a customer and returns {customerCode, firstAccountNumber}. */
    private String[] register(String name, String email, String phone) throws Exception {
        ResultActions result = post("/api/customers",
                "{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"phone\":\"" + phone + "\",\"pin\":\"1234\"}");
        result.andExpect(status().isCreated());
        String json = body(result);
        return new String[]{extract(json, "customerCode"), extract(json, "accountNumber")};
    }

    private void deposit(String[] customer, String amount) throws Exception {
        post("/api/accounts/" + customer[1] + "/deposits", "{\"amount\":" + amount + "}",
                login(customer[0], "1234")).andExpect(status().isCreated());
    }

    private String transferJson(String from, String to, String amount) {
        return "{\"fromAccount\":\"" + from + "\",\"toAccount\":\"" + to + "\",\"amount\":" + amount + "}";
    }

    // ---------- registration and login ----------
    @Test
    void registerReturns201AndNeverShowsThePin() throws Exception {
        ResultActions result = post("/api/customers",
                "{\"name\":\"Alice\",\"email\":\"alice@test.com\",\"phone\":\"01700000001\",\"pin\":\"1234\"}");
        result.andExpect(status().isCreated());
        String json = body(result);
        assertTrue(json.contains("customerCode"));
        assertTrue(json.contains("firstAccount"));
        assertFalse(json.toLowerCase().contains("pin"));
    }

    @Test
    void invalidRegistrationReturns400WithFieldErrors() throws Exception {
        ResultActions result = post("/api/customers",
                "{\"name\":\"\",\"email\":\"not-an-email\",\"phone\":\"\",\"pin\":\"1\"}");
        result.andExpect(status().isBadRequest());
        String json = body(result);
        assertTrue(json.contains("fieldErrors"));
        assertTrue(json.contains("email"));
    }

    @Test
    void wrongPinReturns401() throws Exception {
        String[] alice = register("Alice", "alice@test.com", "01700000001");
        get("/api/accounts", login(alice[0], "0000")).andExpect(status().isUnauthorized());
    }

    @Test
    void missingHeadersReturn401() throws Exception {
        ResultActions result = get("/api/accounts");
        result.andExpect(status().isUnauthorized());
        assertTrue(body(result).contains("Missing header"));
    }

    @Test
    void customerCanListAccounts() throws Exception {
        String[] alice = register("Alice", "alice@test.com", "01700000001");
        ResultActions result = get("/api/accounts", login(alice[0], "1234"));
        result.andExpect(status().isOk());
        assertTrue(body(result).contains(alice[1]));
    }

    // ---------- the full transfer flow ----------
    @Test
    void fullTransferFlowOverHttp() throws Exception {
        String[] alice = register("Alice", "alice@test.com", "01700000001");
        String[] bob = register("Bob", "bob@test.com", "01700000002");
        deposit(alice, "1000");
        deposit(bob, "100");

        // step 1: request (the response shows the fee and total)
        ResultActions created = post("/api/transfers", transferJson(alice[1], bob[1], "500"), login(alice[0], "1234"));
        created.andExpect(status().isCreated());
        String createdJson = body(created);
        assertTrue(createdJson.contains("PENDING_OTP"));
        assertTrue(createdJson.contains("\"fee\":9.25"));
        String requestCode = extract(createdJson, "requestCode");

        // step 2: wrong OTP, then the right one
        post("/api/requests/" + requestCode + "/verify-otp", "{\"otp\":\"000000\"}", login(alice[0], "1234"))
                .andExpect(status().isBadRequest());
        ResultActions verified = post("/api/requests/" + requestCode + "/verify-otp",
                "{\"otp\":\"" + OTP + "\"}", login(alice[0], "1234"));
        verified.andExpect(status().isOk());
        assertTrue(body(verified).contains("PENDING_CONFIRMATION"));

        // step 3: confirm
        ResultActions confirmed = post("/api/requests/" + requestCode + "/confirm", "", login(alice[0], "1234"));
        confirmed.andExpect(status().isOk());
        String confirmedJson = body(confirmed);
        assertTrue(confirmedJson.contains("TRANSFER"));
        assertTrue(confirmedJson.contains("SUCCESS"));

        // balances, history and the admin fee report
        assertTrue(body(get("/api/accounts", login(alice[0], "1234"))).contains("\"balance\":490.75"));
        assertTrue(body(get("/api/accounts", login(bob[0], "1234"))).contains("\"balance\":600.00"));
        assertTrue(body(get("/api/accounts/" + alice[1] + "/transactions", login(alice[0], "1234")))
                .contains("TRANSFER"));
        ResultActions fees = get("/api/admin/fees", ADMIN);
        fees.andExpect(status().isOk());
        assertTrue(body(fees).contains("\"totalFeesCollected\":9.25"));
    }

    @Test
    void cancelledRequestMovesNoMoney() throws Exception {
        String[] alice = register("Alice", "alice@test.com", "01700000001");
        String[] bob = register("Bob", "bob@test.com", "01700000002");
        deposit(alice, "1000");

        String requestCode = extract(body(post("/api/transfers", transferJson(alice[1], bob[1], "500"),
                login(alice[0], "1234"))), "requestCode");
        ResultActions cancelled = post("/api/requests/" + requestCode + "/cancel", "", login(alice[0], "1234"));
        cancelled.andExpect(status().isOk());
        assertTrue(body(cancelled).contains("cancelled"));
        assertTrue(body(get("/api/accounts", login(alice[0], "1234"))).contains("\"balance\":1000.00"));
    }

    // ---------- error responses ----------
    @Test
    void insufficientFundsReturns422() throws Exception {
        String[] alice = register("Alice", "alice@test.com", "01700000001");
        post("/api/withdrawals", "{\"accountNumber\":\"" + alice[1] + "\",\"amount\":500}", login(alice[0], "1234"))
                .andExpect(status().is(422));
    }

    @Test
    void limitExceededReturns422() throws Exception {
        String[] alice = register("Alice", "alice@test.com", "01700000001");
        String[] bob = register("Bob", "bob@test.com", "01700000002");
        deposit(alice, "5000");
        ResultActions result = post("/api/transfers", transferJson(alice[1], bob[1], "2500"), login(alice[0], "1234"));
        result.andExpect(status().is(422));
        assertTrue(body(result).contains("per-transaction limit"));
    }

    @Test
    void unknownAccountReturns404() throws Exception {
        String[] alice = register("Alice", "alice@test.com", "01700000001");
        deposit(alice, "1000");
        post("/api/transfers", transferJson(alice[1], "ACC9999999", "100"), login(alice[0], "1234"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidJsonReturns400() throws Exception {
        String[] alice = register("Alice", "alice@test.com", "01700000001");
        post("/api/transfers", "this is not json", login(alice[0], "1234")).andExpect(status().isBadRequest());
    }

    @Test
    void unknownUrlReturns404() throws Exception {
        get("/api/does-not-exist").andExpect(status().isNotFound());
    }

    // ---------- admin ----------
    @Test
    void adminEndpointsNeedTheAdminPassword() throws Exception {
        get("/api/admin/customers", "X-Admin-Username", "admin", "X-Admin-Password", "wrong")
                .andExpect(status().isUnauthorized());
        get("/api/admin/customers", ADMIN).andExpect(status().isOk());
    }

    @Test
    void frozenAccountReturns403() throws Exception {
        String[] alice = register("Alice", "alice@test.com", "01700000001");
        patch("/api/admin/accounts/" + alice[1] + "/freeze", ADMIN).andExpect(status().isOk());
        post("/api/accounts/" + alice[1] + "/deposits", "{\"amount\":100}", login(alice[0], "1234"))
                .andExpect(status().isForbidden());

        patch("/api/admin/accounts/" + alice[1] + "/unfreeze", ADMIN).andExpect(status().isOk());
        post("/api/accounts/" + alice[1] + "/deposits", "{\"amount\":100}", login(alice[0], "1234"))
                .andExpect(status().isCreated());
    }
}