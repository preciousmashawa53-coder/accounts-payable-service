package zw.co.preciousmashawa.ap.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import zw.co.preciousmashawa.ap.domain.AuditEvent;
import zw.co.preciousmashawa.ap.domain.Invoice;
import zw.co.preciousmashawa.ap.domain.Payment;
import zw.co.preciousmashawa.ap.domain.Supplier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Request and response bodies. Entities are never exposed directly. */
public final class Dtos {

    private Dtos() {
    }

    public record CreateSupplierRequest(
            @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Z0-9-]+", message = "use capital letters, numbers and dashes") String code,
            @NotBlank @Size(max = 120) String name,
            @Email @Size(max = 120) String email) {
    }

    public record CaptureInvoiceRequest(
            @NotBlank String supplierCode,
            @NotBlank @Size(max = 40) String invoiceNumber,
            @Size(max = 40) String poNumber,
            @NotNull LocalDate invoiceDate,
            @NotNull LocalDate dueDate,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount) {
    }

    public record PaymentRequest(
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
            @NotNull LocalDate paymentDate,
            @NotBlank @Size(max = 60) String reference) {
    }

    public record SupplierResponse(Long id, String code, String name, String email) {
        public static SupplierResponse from(Supplier s) {
            return new SupplierResponse(s.getId(), s.getCode(), s.getName(), s.getEmail());
        }
    }

    public record InvoiceResponse(Long id, String supplierCode, String supplierName, String invoiceNumber,
                                  String poNumber, LocalDate invoiceDate, LocalDate dueDate, BigDecimal amount,
                                  BigDecimal amountPaid, BigDecimal outstanding, String status,
                                  String capturedBy, String approvedBy) {
        public static InvoiceResponse from(Invoice i) {
            return new InvoiceResponse(i.getId(), i.getSupplier().getCode(), i.getSupplier().getName(),
                    i.getInvoiceNumber(), i.getPoNumber(), i.getInvoiceDate(), i.getDueDate(), i.getAmount(),
                    i.getAmountPaid(), i.getOutstanding(), i.getStatus().name(), i.getCapturedBy(), i.getApprovedBy());
        }
    }

    public record PaymentResponse(Long id, Long invoiceId, BigDecimal amount, LocalDate paymentDate,
                                  String reference, String paidBy) {
        public static PaymentResponse from(Payment p) {
            return new PaymentResponse(p.getId(), p.getInvoice().getId(), p.getAmount(), p.getPaymentDate(),
                    p.getReference(), p.getPaidBy());
        }
    }

    public record AuditResponse(Long id, String actor, String action, String entityType, Long entityId,
                                String details, LocalDateTime occurredAt) {
        public static AuditResponse from(AuditEvent a) {
            return new AuditResponse(a.getId(), a.getActor(), a.getAction(), a.getEntityType(), a.getEntityId(),
                    a.getDetails(), a.getOccurredAt());
        }
    }

    public record ErrorResponse(int status, String error, String message) {
    }
}
