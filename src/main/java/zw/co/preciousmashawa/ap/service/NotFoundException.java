package zw.co.preciousmashawa.ap.service;

/** The requested record does not exist. Returned as HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
