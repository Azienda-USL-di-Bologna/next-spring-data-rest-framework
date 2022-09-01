package it.nextsw.common.utils.exceptions;

/**
 *
 * @author gdm
 */
public class EntityReflectionException extends Exception {

    public EntityReflectionException() {
    }

    public EntityReflectionException(String message) {
        super(message);
    }

    public EntityReflectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntityReflectionException(Throwable cause) {
        super(cause);
    }

    public EntityReflectionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
