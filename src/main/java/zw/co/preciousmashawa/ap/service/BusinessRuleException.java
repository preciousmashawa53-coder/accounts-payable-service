package zw.co.preciousmashawa.ap.service;

/** A request that breaks an accounting rule, e.g. overpaying an invoice. Returned as HTTP 422. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
