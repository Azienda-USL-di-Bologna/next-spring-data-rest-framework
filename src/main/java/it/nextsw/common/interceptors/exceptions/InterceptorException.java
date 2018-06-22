package it.nextsw.common.interceptors.exceptions;

/**
 *
 * @author spritz
 */
public class InterceptorException extends Exception {

    public InterceptorException() {
    }

    public InterceptorException(String message) {
        super(message);
    }

    public InterceptorException(String message, Throwable cause) {
        super(message, cause);
    }

    public InterceptorException(Throwable cause) {
        super(cause);
    }
}
