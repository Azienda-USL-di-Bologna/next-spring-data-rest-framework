package it.nextsw.common.repositories.exceptions;

/**
 *
 * @author spritz
 */
public class InvalidFilterException extends Exception {

    public InvalidFilterException() {
    }

    public InvalidFilterException(String message) {
        super(message);
    }

    public InvalidFilterException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidFilterException(Throwable cause) {
        super(cause);
    }
}
