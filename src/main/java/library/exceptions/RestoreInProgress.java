package library.exceptions;

/**
 * Exception to manage the attempt to initialize
 * a connection that has already been initialized
 * */
public class RestoreInProgress extends Exception {
    public RestoreInProgress(String message) {
        super(message);
    }
}
