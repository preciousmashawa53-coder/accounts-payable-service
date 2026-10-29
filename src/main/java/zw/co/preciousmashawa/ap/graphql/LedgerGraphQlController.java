package zw.co.preciousmashawa.ap.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import zw.co.preciousmashawa.ap.domain.InvoiceStatus;
import zw.co.preciousmashawa.ap.report.AgingReportService;
import zw.co.preciousmashawa.ap.report.AgingRow;
import zw.co.preciousmashawa.ap.service.AccountsPayableService;
import zw.co.preciousmashawa.ap.service.NotFoundException;
import zw.co.preciousmashawa.ap.web.Dtos.InvoiceResponse;
import zw.co.preciousmashawa.ap.web.Dtos.PaymentResponse;
import zw.co.preciousmashawa.ap.web.Dtos.SupplierResponse;

import java.time.LocalDate;
import java.util.List;

/** GraphQL queries at /graphql, so reporting tools can ask for exactly the fields they need. */
@Controller
public class LedgerGraphQlController {

    private final AccountsPayableService service;
    private final AgingReportService aging;

    public LedgerGraphQlController(AccountsPayableService service, AgingReportService aging) {
        this.service = service;
        this.aging = aging;
    }

    @QueryMapping
    public List<SupplierResponse> suppliers() {
        return service.listSuppliers();
    }

    @QueryMapping
    public List<InvoiceResponse> invoices(@Argument String status) {
        return service.listInvoices(status == null ? null : InvoiceStatus.valueOf(status));
    }

    @QueryMapping
    public InvoiceResponse invoice(@Argument Long id) {
        try {
            return service.getInvoice(id);
        } catch (NotFoundException e) {
            return null;
        }
    }

    @QueryMapping
    public List<PaymentResponse> payments(@Argument Long invoiceId) {
        return service.listPayments(invoiceId);
    }

    @QueryMapping
    public List<AgingRow> agingReport(@Argument String asOf) {
        return aging.agingAsOf(asOf == null ? LocalDate.now() : LocalDate.parse(asOf));
    }
}
