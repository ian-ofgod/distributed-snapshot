package library.exceptions;

/**
 * Exception to manage the attempt to utilize
 * a connection that has not been initialized
 * */
public class NotInitialized extends Exception {
    public NotInitialized(String message) {
        super(message);
    }
}
