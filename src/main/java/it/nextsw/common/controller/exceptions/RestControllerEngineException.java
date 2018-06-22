package it.nextsw.common.controller.exceptions;

/**
 *
 * @author spritz
 */
public class RestControllerEngineException extends Exception {

    public RestControllerEngineException() {
    }

    public RestControllerEngineException(String message) {
        super(message);
    }

    public RestControllerEngineException(String message, Throwable cause) {
        super(message, cause);
    }

    public RestControllerEngineException(Throwable cause) {
        super(cause);
    }
}
