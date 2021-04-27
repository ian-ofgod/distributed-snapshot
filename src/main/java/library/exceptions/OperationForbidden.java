package library.exceptions;

/**
 * Exception to manage the execution of an
 * operation that is not allowed. In particular,
 * it manages the removal of a connection while snapshots are running
 * */
public class OperationForbidden extends Exception{
    public OperationForbidden(String message) {
        super(message);
    }
}
