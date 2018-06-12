package it.nextsw.common.interceptors.exceptions;

/**
 *
 * @author Utente
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
