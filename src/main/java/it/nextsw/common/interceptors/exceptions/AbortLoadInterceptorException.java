package it.nextsw.common.interceptors.exceptions;

/**
 *
 * @author spritz
 */
public class AbortLoadInterceptorException extends Exception {
    
    public AbortLoadInterceptorException() {
        super();
    }

    public AbortLoadInterceptorException(String message) {
        super(message);
    }

    public AbortLoadInterceptorException(String message, Throwable cause) {
        super(message, cause);
    }

    public AbortLoadInterceptorException(Throwable cause) {
        super(cause);
    }
}
