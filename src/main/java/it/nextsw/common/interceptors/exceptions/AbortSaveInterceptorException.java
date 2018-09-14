package it.nextsw.common.interceptors.exceptions;

/**
 *
 * @author spritz
 */
public class AbortSaveInterceptorException extends Exception {
    
    public AbortSaveInterceptorException() {
        super();
    }

    public AbortSaveInterceptorException(String message) {
        super(message);
    }

    public AbortSaveInterceptorException(String message, Throwable cause) {
        super(message, cause);
    }

    public AbortSaveInterceptorException(Throwable cause) {
        super(cause);
    }
}
