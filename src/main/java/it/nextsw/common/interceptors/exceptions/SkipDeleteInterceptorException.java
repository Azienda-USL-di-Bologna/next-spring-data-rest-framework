package it.nextsw.common.interceptors.exceptions;

/**
 *
 * @author spritz
 */
public class SkipDeleteInterceptorException extends Exception {
    
    public SkipDeleteInterceptorException() {
        super();
    }

    public SkipDeleteInterceptorException(String message) {
        super(message);
    }

    public SkipDeleteInterceptorException(String message, Throwable cause) {
        super(message, cause);
    }

    public SkipDeleteInterceptorException(Throwable cause) {
        super(cause);
    }
}
