package zw.co.preciousmashawa.ap.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import zw.co.preciousmashawa.ap.repository.SupplierRepository;
import zw.co.preciousmashawa.ap.service.AccountsPayableService;
import zw.co.preciousmashawa.ap.web.Dtos.CaptureInvoiceRequest;
import zw.co.preciousmashawa.ap.web.Dtos.CreateSupplierRequest;
import zw.co.preciousmashawa.ap.web.Dtos.PaymentRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Loads a few sample suppliers and invoices so the API has data to show in a demo. */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private final AccountsPayableService service;
    private final SupplierRepository suppliers;

    public DemoDataSeeder(AccountsPayableService service, SupplierRepository suppliers) {
        this.service = service;
        this.suppliers = suppliers;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (suppliers.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        service.createSupplier(new CreateSupplierRequest("SUP-001", "Harare Office Supplies", "accounts@hos.example"), "system");
        service.createSupplier(new CreateSupplierRequest("SUP-002", "Mutare IT Solutions", "billing@mits.example"), "system");

        var a = service.captureInvoice(new CaptureInvoiceRequest("SUP-001", "INV-1001", "PO-501",
                today.minusDays(50), today.minusDays(20), new BigDecimal("1250.00")), "clerk");
        service.approve(a.id(), "approver");
        service.pay(a.id(), new PaymentRequest(new BigDecimal("250.00"), today.minusDays(5), "EFT-7781"), "treasury");

        var b = service.captureInvoice(new CaptureInvoiceRequest("SUP-002", "MITS-332", "PO-502",
                today.minusDays(10), today.plusDays(20), new BigDecimal("4800.00")), "clerk");
        service.approve(b.id(), "approver");

        service.captureInvoice(new CaptureInvoiceRequest("SUP-002", "MITS-341", "PO-509",
                today.minusDays(2), today.plusDays(28), new BigDecimal("960.00")), "clerk");
    }
}
