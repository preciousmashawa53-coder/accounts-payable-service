package zw.co.preciousmashawa.ap.service;

/** The record already exists, e.g. a duplicate supplier invoice. Returned as HTTP 409. */
public class DuplicateException extends RuntimeException {

    public DuplicateException(String message) {
        super(message);
    }
}
