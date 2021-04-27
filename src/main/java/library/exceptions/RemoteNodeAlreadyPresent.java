package library.exceptions;

/**
 * Exception to manage the attempt to connect
 * to a node that is already connected
 * */
public class RemoteNodeAlreadyPresent extends Exception{
    public RemoteNodeAlreadyPresent(String message) {
        super(message);
    }
}
