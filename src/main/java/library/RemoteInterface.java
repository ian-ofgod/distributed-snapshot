package library;

import library.exceptions.DoubleMarkerException;
import library.exceptions.SnapshotInterruptException;
import library.exceptions.UnexpectedMarkerReceived;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * This interface is implementing the RMI Remote Interface.
 * It is providing the methods that are necessary for communication.
 * Its implementation enables the reception of messages, the reception
 * of markers, the creation of bidirectional connections and the
 * removal of connections
 * */
interface RemoteInterface<MessageType> extends Remote {

    /**
     * This method is called from a remote node to send a message. If the sender node is not present in the remote node list removeMe() is invoked.
     * If one (or more than one) snapshot is running it checks if it has to save the received message inside the snapshot
     * @param senderIp the hostname of the entity that sent the message that is being received
     * @param senderPort the RMI registry port of the entity that sent the message that is being received
     * @param message the message that is being received
     * @throws RemoteException communication-related exception that may occur during remote calls
     * @throws NotBoundException the remote node that is being removed has not bound its remote implementation
     * @throws SnapshotInterruptException it's not possible to remove a node when a snapshot is running
            * */
    void receiveMessage(String senderIp, int senderPort, MessageType message) throws RemoteException, NotBoundException, SnapshotInterruptException;

    /**
     * It is called from a remote node to send a marker of a running snapshot on the network
     * @param senderHostname the hostname of the entity that sent the marker that is being received
     * @param senderPort the RMI registry port of the entity that sent the marker that is being received
     * @param initiatorHostname the hostname of the entity that initiated the snapshot
     * @param initiatorPort the RMI registry port of the entity that initiated the snapshot
     * @param snapshotId the unique snapshot identifier (i.e. marker) that is being received
     * @throws DoubleMarkerException received multiple marker (same id) from the same link
     * @throws UnexpectedMarkerReceived the sender node is not present in the remote nodes list
     * */
    void receiveMarker(String senderHostname, int senderPort, String initiatorHostname, int initiatorPort, int snapshotId) throws RemoteException, DoubleMarkerException, UnexpectedMarkerReceived;

    /**
     * This method is called from a remote node to add itself to the remote node list of the local node (this one)
     * @param hostname the hostname of the entity that should be added
     * @param port the RMI registry port of the entity that should be added
     * @throws RemoteException communication-related exception that may occur during remote calls
     * @throws NotBoundException the remote node has not bound its remote implementation
     */
    void addMeBack(String hostname, int port) throws RemoteException, NotBoundException;

    /**
     * This method is called from a remote node to delete itself from the remote node list of the local node (this one)
     * @param hostname the hostname of the entity that should be removed
     * @param port the RMI registry port of the entity that should be removed
     * @throws RemoteException communication-related exception that may occur during remote calls
     * @throws SnapshotInterruptException it's not possible to remove a node when a snapshot is running
     */
    void removeMe(String hostname, int port) throws RemoteException, SnapshotInterruptException;
}