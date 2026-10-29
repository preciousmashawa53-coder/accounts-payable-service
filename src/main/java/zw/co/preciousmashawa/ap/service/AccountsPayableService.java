package zw.co.preciousmashawa.ap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.preciousmashawa.ap.domain.AuditEvent;
import zw.co.preciousmashawa.ap.domain.Invoice;
import zw.co.preciousmashawa.ap.domain.InvoiceStatus;
import zw.co.preciousmashawa.ap.domain.Payment;
import zw.co.preciousmashawa.ap.domain.Supplier;
import zw.co.preciousmashawa.ap.repository.AuditEventRepository;
import zw.co.preciousmashawa.ap.repository.InvoiceRepository;
import zw.co.preciousmashawa.ap.repository.PaymentRepository;
import zw.co.preciousmashawa.ap.repository.SupplierRepository;
import zw.co.preciousmashawa.ap.web.Dtos.AuditResponse;
import zw.co.preciousmashawa.ap.web.Dtos.CaptureInvoiceRequest;
import zw.co.preciousmashawa.ap.web.Dtos.CreateSupplierRequest;
import zw.co.preciousmashawa.ap.web.Dtos.InvoiceResponse;
import zw.co.preciousmashawa.ap.web.Dtos.PaymentRequest;
import zw.co.preciousmashawa.ap.web.Dtos.PaymentResponse;
import zw.co.preciousmashawa.ap.web.Dtos.SupplierResponse;

import java.util.List;

/**
 * Accounts payable workflow: capture supplier invoices, approve or reject them,
 * record payments, and keep an audit trail of every change.
 */
@Service
public class AccountsPayableService {

    private final SupplierRepository suppliers;
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final AuditEventRepository audit;

    public AccountsPayableService(SupplierRepository suppliers, InvoiceRepository invoices,
                                  PaymentRepository payments, AuditEventRepository audit) {
        this.suppliers = suppliers;
        this.invoices = invoices;
        this.payments = payments;
        this.audit = audit;
    }

    // ---------------------------------------------------------------- suppliers

    @Transactional
    public SupplierResponse createSupplier(CreateSupplierRequest request, String user) {
        if (suppliers.existsByCode(request.code())) {
            throw new DuplicateException("A supplier with code " + request.code() + " already exists.");
        }
        Supplier supplier = suppliers.save(new Supplier(request.code(), request.name(), request.email()));
        record(user, "SUPPLIER_CREATED", "Supplier", supplier.getId(), supplier.getCode() + " " + supplier.getName());
        return SupplierResponse.from(supplier);
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> listSuppliers() {
        return suppliers.findAllByOrderByCodeAsc().stream().map(SupplierResponse::from).toList();
    }

    // ---------------------------------------------------------------- invoices

    @Transactional
    public InvoiceResponse captureInvoice(CaptureInvoiceRequest request, String user) {
        Supplier supplier = suppliers.findByCode(request.supplierCode())
                .orElseThrow(() -> new NotFoundException("Supplier " + request.supplierCode() + " was not found."));
        if (invoices.existsBySupplierIdAndInvoiceNumber(supplier.getId(), request.invoiceNumber())) {
            throw new DuplicateException("Invoice " + request.invoiceNumber() + " from " + supplier.getCode()
                    + " has already been captured.");
        }
        Invoice invoice = invoices.save(new Invoice(supplier, request.invoiceNumber(), request.poNumber(),
                request.invoiceDate(), request.dueDate(), request.amount(), user));
        record(user, "INVOICE_CAPTURED", "Invoice", invoice.getId(),
                supplier.getCode() + " " + invoice.getInvoiceNumber() + " amount " + invoice.getAmount());
        return InvoiceResponse.from(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listInvoices(InvoiceStatus status) {
        List<Invoice> found = status == null ? invoices.findAllWithSupplier() : invoices.findByStatusWithSupplier(status);
        return found.stream().map(InvoiceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long id) {
        return InvoiceResponse.from(find(id));
    }

    @Transactional
    public InvoiceResponse approve(Long id, String user) {
        Invoice invoice = find(id);
        invoice.approve(user);
        record(user, "INVOICE_APPROVED", "Invoice", id, null);
        return InvoiceResponse.from(invoice);
    }

    @Transactional
    public InvoiceResponse reject(Long id, String user) {
        Invoice invoice = find(id);
        invoice.reject(user);
        record(user, "INVOICE_REJECTED", "Invoice", id, null);
        return InvoiceResponse.from(invoice);
    }

    // ---------------------------------------------------------------- payments

    @Transactional
    public PaymentResponse pay(Long invoiceId, PaymentRequest request, String user) {
        Invoice invoice = find(invoiceId);
        invoice.applyPayment(request.amount());
        Payment payment = payments.save(new Payment(invoice, request.amount(), request.paymentDate(),
                request.reference(), user));
        record(user, "PAYMENT_RECORDED", "Invoice", invoiceId,
                "paid " + request.amount() + " ref " + request.reference() + ", outstanding " + invoice.getOutstanding());
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listPayments(Long invoiceId) {
        find(invoiceId);
        return payments.findByInvoiceIdOrderByIdAsc(invoiceId).stream().map(PaymentResponse::from).toList();
    }

    // ---------------------------------------------------------------- audit

    @Transactional(readOnly = true)
    public List<AuditResponse> recentAudit() {
        return audit.findTop100ByOrderByIdDesc().stream().map(AuditResponse::from).toList();
    }

    private Invoice find(Long id) {
        return invoices.findById(id).orElseThrow(() -> new NotFoundException("Invoice " + id + " was not found."));
    }

    private void record(String actor, String action, String entityType, Long entityId, String details) {
        audit.save(new AuditEvent(actor, action, entityType, entityId, details));
    }
}
