package it.nextsw.common.controller.exceptions;

/**
 *
 * @author gdm
 */
public class BeforeUpdateEntityApplierException extends Exception {

    public BeforeUpdateEntityApplierException() {
    }

    public BeforeUpdateEntityApplierException(String message) {
        super(message);
    }

    public BeforeUpdateEntityApplierException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeforeUpdateEntityApplierException(Throwable cause) {
        super(cause);
    }
}
