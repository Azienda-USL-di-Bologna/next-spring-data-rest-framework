package it.nextsw.common.interceptors.exceptions;

/**
 *
 * @author spritz
 */
public class RollBackInterceptorException extends Exception {

    public RollBackInterceptorException() {
    }

    public RollBackInterceptorException(String message) {
        super(message);
    }

    public RollBackInterceptorException(String message, Throwable cause) {
        super(message, cause);
    }

    public RollBackInterceptorException(Throwable cause) {
        super(cause);
    }
}
