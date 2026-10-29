package zw.co.preciousmashawa.ap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import zw.co.preciousmashawa.ap.service.BusinessRuleException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A supplier invoice. Business rules that protect the ledger live here,
 * so an invoice can never reach an invalid state whichever service calls it.
 */
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "invoice_number", nullable = false, length = 40)
    private String invoiceNumber;

    @Column(name = "po_number", length = 40)
    private String poNumber;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "amount_paid", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(name = "captured_by", nullable = false, length = 60)
    private String capturedBy;

    @Column(name = "approved_by", length = 60)
    private String approvedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Invoice() {
        // for JPA
    }

    public Invoice(Supplier supplier, String invoiceNumber, String poNumber, LocalDate invoiceDate,
                   LocalDate dueDate, BigDecimal amount, String capturedBy) {
        if (dueDate.isBefore(invoiceDate)) {
            throw new BusinessRuleException("The due date cannot be before the invoice date.");
        }
        if (amount.signum() <= 0) {
            throw new BusinessRuleException("The invoice amount must be greater than zero.");
        }
        this.supplier = supplier;
        this.invoiceNumber = invoiceNumber;
        this.poNumber = poNumber;
        this.invoiceDate = invoiceDate;
        this.dueDate = dueDate;
        this.amount = amount;
        this.capturedBy = capturedBy;
        this.status = InvoiceStatus.CAPTURED;
        this.createdAt = LocalDateTime.now();
    }

    /** Segregation of duties: the person who captured an invoice may not approve it. */
    public void approve(String approver) {
        requireStatus(InvoiceStatus.CAPTURED, "approve");
        if (approver.equals(capturedBy)) {
            throw new BusinessRuleException("You captured this invoice, so another person must approve it.");
        }
        this.status = InvoiceStatus.APPROVED;
        this.approvedBy = approver;
    }

    public void reject(String approver) {
        requireStatus(InvoiceStatus.CAPTURED, "reject");
        this.status = InvoiceStatus.REJECTED;
        this.approvedBy = approver;
    }

    /** Records a payment. Partial payments are allowed; overpayment is not. */
    public void applyPayment(BigDecimal payment) {
        if (!status.isOutstanding()) {
            throw new BusinessRuleException("Only approved invoices with a balance can be paid. This invoice is " + status + ".");
        }
        if (payment.signum() <= 0) {
            throw new BusinessRuleException("The payment amount must be greater than zero.");
        }
        if (payment.compareTo(getOutstanding()) > 0) {
            throw new BusinessRuleException("The payment of " + payment + " is more than the outstanding balance of " + getOutstanding() + ".");
        }
        this.amountPaid = amountPaid.add(payment);
        this.status = getOutstanding().signum() == 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID;
    }

    private void requireStatus(InvoiceStatus expected, String action) {
        if (status != expected) {
            throw new BusinessRuleException("Cannot " + action + " an invoice that is " + status + ".");
        }
    }

    public BigDecimal getOutstanding() {
        return amount.subtract(amountPaid);
    }

    public Long getId() {
        return id;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public String getCapturedBy() {
        return capturedBy;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
