package library.exceptions;

/**
 * Exception to manage issues in updating the state
 * */
public class StateUpdateException extends Exception {
    public StateUpdateException(String message) {
        super(message);
    }
}
