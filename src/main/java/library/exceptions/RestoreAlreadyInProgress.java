package library.exceptions;

/**
 * Exception that it's raised when an attempt to restore two different snapshots occurs
 * */
public class RestoreAlreadyInProgress extends Exception {
    public RestoreAlreadyInProgress(String message) {
        super(message);
    }
}
