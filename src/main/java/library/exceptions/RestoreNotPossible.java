package library.exceptions;

/**
 * Exception that it's raised when an attempt to restore two different snapshots occurs
 * */
public class RestoreNotPossible extends Exception {
    public RestoreNotPossible(String message) {
        super(message);
    }
}
