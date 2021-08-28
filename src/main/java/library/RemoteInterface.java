package library;

import library.exceptions.*;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

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
     * @param senderHostname the hostname of the entity that sent the message that is being received
     * @param senderPort the RMI registry port of the entity that sent the message that is being received
     * @param message the message that is being received
     * @throws RemoteException communication-related exception that may occur during remote calls
     * @throws NotBoundException the remote node that is being removed has not bound its remote implementation
     * @throws SnapshotInterruptException it's not possible to remove a node when a snapshot is running
     * */
    void receiveMessage(String senderHostname, int senderPort, MessageType message) throws RemoteException, NotBoundException, SnapshotInterruptException;

    /**
     * It is called from a remote node to send a marker of a running snapshot on the network
     * @param senderHostname the hostname of the entity that sent the marker that is being received
     * @param senderPort the RMI registry port of the entity that sent the marker that is being received
     * @param initiatorHostname the hostname of the entity that initiated the snapshot
     * @param initiatorPort the RMI registry port of the entity that initiated the snapshot
     * @param snapshotId the unique snapshot identifier (i.e. marker) that is being received
     * @throws DoubleMarkerException received multiple marker (same id) from the same link
     * @throws UnexpectedMarkerReceived the sender node is not present in the remote nodes list
     * @throws IOException an error is occurred while using the storage facility to write the snapshot to disk or communication-related exception that may occur during remote calls
     * */
    void receiveMarker(String senderHostname, int senderPort, String initiatorHostname, int initiatorPort, int snapshotId) throws IOException, DoubleMarkerException, UnexpectedMarkerReceived;

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

    /**
     * This method is called from a remote node to get the list of connections as an arrayList of Entities
     * @return an ArrayList containing Entities a class storing hostname and port of a node
     * @throws RemoteException communication-related exception that may occur during remote calls
     */
    ArrayList<Entity> getConnections() throws RemoteException;

    /**
     *  This method is called from a remote node to restore the state of the current node to the one of the provided snapshotId
     * @param snapshotId the id of the selected snapshot
     * @throws RemoteException communication-related exception that may occur during remote calls
     * @throws RestoreAlreadyInProgress thrown if a restore with a different snapshotId is already in progress
     * @throws IOException an error is occurred while using the storage facility to write the snapshot to disk or communication-related exception that may occur during remote calls
     */
    void restoreState(int snapshotId) throws IOException, RestoreAlreadyInProgress, ClassNotFoundException;

    /**
     * This method is called from a remote node to restore the connections of the current node to the ones of the provided snapshotId
     * @param snapshotId the id of the selected snapshot
     * @throws RemoteException communication-related exception that may occur during remote calls
     * @throws RestoreAlreadyInProgress thrown if a restore with a different snapshotId is already in progress
     * @throws NotBoundException thrown if an attempt is made to lookup or unbind in the registry a name that has no associated binding.
     * @throws RestoreNotPossible thrown if a restore is not possible, in the exception message the reason is provided (for example a node is no more reachable)
     * @throws IOException an error is occurred while using the storage facility to write the snapshot to disk or communication-related exception that may occur during remote calls
     */
    void restoreConnections(int snapshotId) throws IOException, RestoreAlreadyInProgress, NotBoundException, RestoreNotPossible, ClassNotFoundException;

    /**
     * This method is called from a remote node to restore the old incoming messages contained in the snapshot
     * @param snapshotId the id of the selected snapshot
     * @throws IOException an error is occurred while using the storage facility to write the snapshot to disk or communication-related exception that may occur during remote calls
     * @throws RestoreAlreadyInProgress thrown if a restore is already in progress in the remote node
     * @throws ClassNotFoundException thrown if the storage facility is unable to obtain the snapshot from the disk
     */
    void restoreOldIncomingMessages(int snapshotId) throws IOException, RestoreAlreadyInProgress, ClassNotFoundException;

    /**
     * This method is called from a remote node to set our ready state, this state is used to indicate if a node is currently restoring a snapshot or not
     * @param value the value to be set
     * @throws RemoteException communication-related exception that may occur during remote calls
     */
    void setReady(boolean value) throws RemoteException;

    /**
     * This method is called from a remote node to ask us to remove a node from our connections
     * @param hostname the hostname of the node to remove
     * @param port the port of the node to remove
     * @throws RemoteException communication-related exception that may occur during remote calls
     */
    void forgetThisNode(String hostname, int port) throws RemoteException;
}