package library.exceptions;
/**
 * Exception to manage the reception
 * of a marker from a node that is not
 * connected to the host
 * */
public class UnexpectedMarkerReceived extends Exception{
    public UnexpectedMarkerReceived(String message) {
        super(message);
    }
}
