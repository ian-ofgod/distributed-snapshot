package library.exceptions;

/**
 * Exception to manage the case of network broken or node crash
 * during a Snapshot, which violates assumptions
 * */
public class SnapshotInterruptException extends Exception {
    public SnapshotInterruptException(String message) {
        super(message);
    }

}
