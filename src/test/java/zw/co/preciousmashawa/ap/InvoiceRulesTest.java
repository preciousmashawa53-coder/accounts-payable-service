package zw.co.preciousmashawa.ap;

import org.junit.jupiter.api.Test;
import zw.co.preciousmashawa.ap.domain.Invoice;
import zw.co.preciousmashawa.ap.domain.InvoiceStatus;
import zw.co.preciousmashawa.ap.domain.Supplier;
import zw.co.preciousmashawa.ap.service.BusinessRuleException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit tests for the invoice rules, with no Spring or database. */
class InvoiceRulesTest {

    private final Supplier supplier = new Supplier("SUP-T", "Test", null);
    private final LocalDate today = LocalDate.of(2026, 10, 28);

    private Invoice invoice(String amount) {
        return new Invoice(supplier, "T-1", null, today, today.plusDays(30), new BigDecimal(amount), "clerk");
    }

    @Test
    void movesThroughApprovalAndPayment() {
        Invoice invoice = invoice("500.00");
        invoice.approve("approver");
        invoice.applyPayment(new BigDecimal("200.00"));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
        assertThat(invoice.getOutstanding()).isEqualByComparingTo("300.00");
        invoice.applyPayment(new BigDecimal("300.00"));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void blocksSelfApprovalOverpaymentAndBadDates() {
        Invoice invoice = invoice("100.00");
        assertThatThrownBy(() -> invoice.approve("clerk")).isInstanceOf(BusinessRuleException.class);
        invoice.approve("approver");
        assertThatThrownBy(() -> invoice.applyPayment(new BigDecimal("100.01"))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new Invoice(supplier, "T-2", null, today, today.minusDays(1), BigDecimal.TEN, "clerk"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cannotApproveTwiceOrPayARejectedInvoice() {
        Invoice approved = invoice("100.00");
        approved.approve("approver");
        assertThatThrownBy(() -> approved.approve("approver2")).isInstanceOf(BusinessRuleException.class);

        Invoice rejected = invoice("100.00");
        rejected.reject("approver");
        assertThatThrownBy(() -> rejected.applyPayment(BigDecimal.ONE)).isInstanceOf(BusinessRuleException.class);
    }
}
