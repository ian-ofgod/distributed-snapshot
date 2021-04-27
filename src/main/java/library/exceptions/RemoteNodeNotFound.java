package library.exceptions;

/**
 * Exception to manage the attempt to connect
 * to a node that cannot be found
 * */
public class RemoteNodeNotFound extends Exception{
    public RemoteNodeNotFound(String message) {
        super(message);
    }
}
