package library.exceptions;

/**
 * Exception to manage the attempt to initialize
 * a connection that has already been initialized
 * */
public class AlreadyInitialized extends Exception {
    public AlreadyInitialized(String message) {
        super(message);
    }
}
