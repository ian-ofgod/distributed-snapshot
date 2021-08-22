package library.exceptions;

/**
 * Exception to manage the attempt to initialize
 * a connection that has already been initialized
 * */
public class RestoreException extends Exception {
    public RestoreException(String message) {
        super(message);
    }
}
