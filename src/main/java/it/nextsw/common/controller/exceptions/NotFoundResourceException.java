package it.nextsw.common.controller.exceptions;

/**
 *
 * @author gdm
 */
public class NotFoundResourceException extends Exception {

    public NotFoundResourceException() {
    }

    public NotFoundResourceException(String message) {
        super(message);
    }

    public NotFoundResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundResourceException(Throwable cause) {
        super(cause);
    }
}
