package zw.co.preciousmashawa.ap.domain;

/** Lifecycle of a supplier invoice: captured, then approved or rejected, then paid. */
public enum InvoiceStatus {
    CAPTURED,
    APPROVED,
    REJECTED,
    PARTIALLY_PAID,
    PAID;

    /** Invoices in these states still owe money to the supplier and count in the aging report. */
    public boolean isOutstanding() {
        return this == APPROVED || this == PARTIALLY_PAID;
    }
}
