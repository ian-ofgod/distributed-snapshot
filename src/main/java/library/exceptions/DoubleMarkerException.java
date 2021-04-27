package library.exceptions;

/**
 * Exception to manage the reception of a double identical marker
 * which should not happen and clearly indicates problems
 * */
public class DoubleMarkerException extends Exception {
    public DoubleMarkerException(String message) {
        super(message);
    }

}