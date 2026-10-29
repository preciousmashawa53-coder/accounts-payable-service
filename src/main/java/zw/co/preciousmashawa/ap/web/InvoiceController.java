package zw.co.preciousmashawa.ap.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import zw.co.preciousmashawa.ap.domain.InvoiceStatus;
import zw.co.preciousmashawa.ap.service.AccountsPayableService;
import zw.co.preciousmashawa.ap.web.Dtos.CaptureInvoiceRequest;
import zw.co.preciousmashawa.ap.web.Dtos.InvoiceResponse;
import zw.co.preciousmashawa.ap.web.Dtos.PaymentRequest;
import zw.co.preciousmashawa.ap.web.Dtos.PaymentResponse;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final AccountsPayableService service;

    public InvoiceController(AccountsPayableService service) {
        this.service = service;
    }

    @GetMapping
    public List<InvoiceResponse> list(@RequestParam(required = false) InvoiceStatus status) {
        return service.listInvoices(status);
    }

    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable Long id) {
        return service.getInvoice(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLERK')")
    public InvoiceResponse capture(@Valid @RequestBody CaptureInvoiceRequest request, Principal user) {
        return service.captureInvoice(request, user.getName());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('APPROVER')")
    public InvoiceResponse approve(@PathVariable Long id, Principal user) {
        return service.approve(id, user.getName());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('APPROVER')")
    public InvoiceResponse reject(@PathVariable Long id, Principal user) {
        return service.reject(id, user.getName());
    }

    @GetMapping("/{id}/payments")
    public List<PaymentResponse> payments(@PathVariable Long id) {
        return service.listPayments(id);
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TREASURY')")
    public PaymentResponse pay(@PathVariable Long id, @Valid @RequestBody PaymentRequest request, Principal user) {
        return service.pay(id, request, user.getName());
    }
}
