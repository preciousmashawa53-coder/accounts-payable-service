package zw.co.preciousmashawa.ap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.seed-demo-data=false")
@AutoConfigureMockMvc
class AccountsPayableApiTest {

    private static final RequestPostProcessor CLERK = httpBasic("clerk", "clerk-pass");
    private static final RequestPostProcessor APPROVER = httpBasic("approver", "approver-pass");
    private static final RequestPostProcessor TREASURY = httpBasic("treasury", "treasury-pass");
    private static final RequestPostProcessor SUPERVISOR = httpBasic("supervisor", "supervisor-pass");

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    // ---------------------------------------------------------------- helpers

    private String newSupplier() throws Exception {
        String code = "S-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        mvc.perform(post("/api/suppliers").with(CLERK).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\",\"name\":\"Test Supplier\",\"email\":\"ap@test.example\"}"))
                .andExpect(status().isCreated());
        return code;
    }

    private String invoiceJson(String supplier, String number, LocalDate due, String amount) {
        return "{\"supplierCode\":\"" + supplier + "\",\"invoiceNumber\":\"" + number + "\",\"poNumber\":\"PO-1\","
                + "\"invoiceDate\":\"" + due.minusDays(30) + "\",\"dueDate\":\"" + due + "\",\"amount\":" + amount + "}";
    }

    private long capture(String supplier, String number, LocalDate due, String amount, RequestPostProcessor user) throws Exception {
        MvcResult result = mvc.perform(post("/api/invoices").with(user).contentType(MediaType.APPLICATION_JSON)
                        .content(invoiceJson(supplier, number, due, amount)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CAPTURED"))
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String payJson(String amount, String reference) {
        return "{\"amount\":" + amount + ",\"paymentDate\":\"" + LocalDate.now() + "\",\"reference\":\"" + reference + "\"}";
    }

    // ---------------------------------------------------------------- tests

    @Test
    void capturesApprovesAndPaysAnInvoiceInFull() throws Exception {
        String supplier = newSupplier();
        long id = capture(supplier, "INV-1", LocalDate.now().plusDays(30), "1000.00", CLERK);

        mvc.perform(post("/api/invoices/{id}/approve", id).with(APPROVER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedBy").value("approver"));

        mvc.perform(post("/api/invoices/{id}/payments", id).with(TREASURY).contentType(MediaType.APPLICATION_JSON)
                        .content(payJson("400.00", "EFT-1")))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/invoices/{id}", id).with(CLERK))
                .andExpect(jsonPath("$.status").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.outstanding").value(600.0));

        mvc.perform(post("/api/invoices/{id}/payments", id).with(TREASURY).contentType(MediaType.APPLICATION_JSON)
                        .content(payJson("600.00", "EFT-2")))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/invoices/{id}", id).with(CLERK))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.outstanding").value(0.0));

        mvc.perform(get("/api/invoices/{id}/payments", id).with(CLERK))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void rejectsADuplicateInvoiceFromTheSameSupplier() throws Exception {
        String supplier = newSupplier();
        capture(supplier, "DUP-1", LocalDate.now().plusDays(30), "50.00", CLERK);

        mvc.perform(post("/api/invoices").with(CLERK).contentType(MediaType.APPLICATION_JSON)
                        .content(invoiceJson(supplier, "DUP-1", LocalDate.now().plusDays(30), "50.00")))
                .andExpect(status().isConflict());
    }

    @Test
    void doesNotAllowOverpayment() throws Exception {
        String supplier = newSupplier();
        long id = capture(supplier, "OVER-1", LocalDate.now().plusDays(30), "100.00", CLERK);
        mvc.perform(post("/api/invoices/{id}/approve", id).with(APPROVER)).andExpect(status().isOk());

        mvc.perform(post("/api/invoices/{id}/payments", id).with(TREASURY).contentType(MediaType.APPLICATION_JSON)
                        .content(payJson("100.01", "EFT-9")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void doesNotPayAnInvoiceBeforeItIsApproved() throws Exception {
        String supplier = newSupplier();
        long id = capture(supplier, "EARLY-1", LocalDate.now().plusDays(30), "100.00", CLERK);

        mvc.perform(post("/api/invoices/{id}/payments", id).with(TREASURY).contentType(MediaType.APPLICATION_JSON)
                        .content(payJson("10.00", "EFT-3")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void enforcesRoles() throws Exception {
        String supplier = newSupplier();
        long id = capture(supplier, "ROLE-1", LocalDate.now().plusDays(30), "100.00", CLERK);

        mvc.perform(post("/api/invoices/{id}/approve", id).with(CLERK)).andExpect(status().isForbidden());
        mvc.perform(post("/api/invoices/{id}/payments", id).with(APPROVER).contentType(MediaType.APPLICATION_JSON)
                .content(payJson("10.00", "EFT-4"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/invoices")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/invoices").with(httpBasic("clerk", "wrong"))).andExpect(status().isUnauthorized());
    }

    @Test
    void preventsSelfApproval() throws Exception {
        String supplier = newSupplier();
        long id = capture(supplier, "SOD-1", LocalDate.now().plusDays(30), "100.00", SUPERVISOR);

        mvc.perform(post("/api/invoices/{id}/approve", id).with(SUPERVISOR))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/invoices/{id}/approve", id).with(APPROVER))
                .andExpect(status().isOk());
    }

    @Test
    void validatesInput() throws Exception {
        String supplier = newSupplier();
        mvc.perform(post("/api/invoices").with(CLERK).contentType(MediaType.APPLICATION_JSON)
                        .content(invoiceJson(supplier, "BAD-1", LocalDate.now().plusDays(30), "-5")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/invoices").with(CLERK).contentType(MediaType.APPLICATION_JSON)
                        .content(invoiceJson("NO-SUCH-SUPPLIER", "BAD-2", LocalDate.now().plusDays(30), "5")))
                .andExpect(status().isNotFound());
    }

    @Test
    void buildsTheAgingReport() throws Exception {
        String supplier = newSupplier();
        LocalDate asOf = LocalDate.of(2026, 10, 31);
        long notDue = capture(supplier, "AGE-1", asOf.plusDays(10), "100.00", CLERK);
        long late45 = capture(supplier, "AGE-2", asOf.minusDays(45), "300.00", CLERK);
        long late120 = capture(supplier, "AGE-3", asOf.minusDays(120), "50.00", CLERK);
        capture(supplier, "AGE-4", asOf.minusDays(10), "999.00", CLERK); // not approved: excluded
        for (long id : new long[]{notDue, late45, late120}) {
            mvc.perform(post("/api/invoices/{id}/approve", id).with(APPROVER)).andExpect(status().isOk());
        }

        MvcResult result = mvc.perform(get("/api/reports/aging").param("asOf", asOf.toString()).with(CLERK))
                .andExpect(status().isOk()).andReturn();
        JsonNode row = null;
        for (JsonNode r : json.readTree(result.getResponse().getContentAsString())) {
            if (r.get("supplierCode").asText().equals(supplier)) {
                row = r;
            }
        }
        assertThat(row).isNotNull();
        assertThat(row.get("current").decimalValue()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(row.get("days31To60").decimalValue()).isEqualByComparingTo(new BigDecimal("300"));
        assertThat(row.get("over90").decimalValue()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(row.get("total").decimalValue()).isEqualByComparingTo(new BigDecimal("450"));
    }

    @Test
    void recordsAnAuditTrail() throws Exception {
        String supplier = newSupplier();
        long id = capture(supplier, "AUD-1", LocalDate.now().plusDays(30), "75.00", CLERK);
        mvc.perform(post("/api/invoices/{id}/approve", id).with(APPROVER)).andExpect(status().isOk());

        MvcResult result = mvc.perform(get("/api/audit").with(CLERK)).andExpect(status().isOk()).andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("INVOICE_CAPTURED").contains("INVOICE_APPROVED");
    }

    @Test
    void answersGraphQlQueries() throws Exception {
        String supplier = newSupplier();
        long id = capture(supplier, "GQL-1", LocalDate.now().plusDays(30), "250.00", CLERK);

        String query = "{\"query\":\"{ invoice(id: " + id + ") { invoiceNumber supplierCode amount status } }\"}";
        MvcResult result = mvc.perform(post("/graphql").with(CLERK)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).content(query))
                .andReturn();
        // Spring GraphQL answers asynchronously over HTTP; finish the request if it was handed off
        if (result.getRequest().isAsyncStarted()) {
            result = mvc.perform(asyncDispatch(result)).andReturn();
        }
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode invoice = json.readTree(result.getResponse().getContentAsString()).path("data").path("invoice");
        assertThat(invoice.path("invoiceNumber").asText()).isEqualTo("GQL-1");
        assertThat(invoice.path("supplierCode").asText()).isEqualTo(supplier);
        assertThat(invoice.path("status").asText()).isEqualTo("CAPTURED");
        assertThat(invoice.path("amount").decimalValue()).isEqualByComparingTo(new BigDecimal("250"));
    }
}
