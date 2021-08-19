package library;

import library.exceptions.*;

import java.io.*;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Objects;

/**
 * This is the main class of the distributed snapshot library. A DistributedSnapshot object must be created
 * in order to interact with the library. This implementation provides methods such as init, sendMessage,
 * updateState, addConnection, removeConnection and initiateSnapshot.
 * @param <StateType> this is the type that will be used to store the application state
 * @param <MessageType> this is the type that will be exchanged as a message between nodes
 * */
public class DistributedSnapshot<StateType, MessageType> {
    /**
     * The implementation of the remoteInterface used on this node
     * */
    protected final RemoteImplementation<StateType,MessageType> remoteImplementation = new RemoteImplementation<>();

    /**
     * This method is used to initialize a DistributedSnapshot object. It sets the hostname, the port and the appConnector reference.
     * It starts the rmi registry and publishes the RemoteInterface in order to be reachable from other nodes.
     * @param yourHostname the hostname the application can be reached at
     * @param rmiRegistryPort the port used by the rmi registry
     * @param appConnector the reference to an appConnector implementation
     * @throws RemoteException communication-related exception that may occur during remote calls
     * @throws AlreadyBoundException the rmi registry has already bound a remote interface, try with another registry
     * @throws AlreadyInitialized this instance has been already initialized
     */
    public void init(String yourHostname, int rmiRegistryPort, AppConnector<MessageType> appConnector) throws RemoteException, AlreadyBoundException, AlreadyInitialized {
        if (remoteImplementation.appConnector != null) throw new AlreadyInitialized("You are trying to initialize an instance that is already initialized");

        synchronized (remoteImplementation) {
            remoteImplementation.hostname = yourHostname;
            remoteImplementation.port = rmiRegistryPort;
            remoteImplementation.appConnector = appConnector;

            RemoteInterface<MessageType> stub = (RemoteInterface<MessageType>) UnicastRemoteObject.exportObject(remoteImplementation, 0);
            Registry registry = LocateRegistry.createRegistry(remoteImplementation.port);
            registry.bind("RemoteInterface", stub);
        }
    }

    public ArrayList<Entity> joinNetwork(String hostname, int port) throws RemoteException, NotBoundException, NotInitialized {
        if (remoteImplementation.appConnector == null) throw new NotInitialized("Before connecting to (hostname " + hostname +
                "and port " + port +
                ") you must initialize this instance");

        ArrayList<Entity> networkNodes;
        synchronized (remoteImplementation) {
            Registry registry = LocateRegistry.getRegistry(hostname, port);
            RemoteInterface<MessageType> remoteInterface = (RemoteInterface<MessageType>) registry.lookup("RemoteInterface");
            networkNodes = remoteInterface.getNodes();
            remoteImplementation.remoteNodes.add(new RemoteNode<>(hostname, port, remoteInterface));
            remoteInterface.addMeBack(remoteImplementation.hostname, remoteImplementation.port);
            for (Entity entry : networkNodes) {
                Registry nodeRegistry = LocateRegistry.getRegistry(entry.getIpAddress(), entry.getPort());
                RemoteInterface<MessageType> nodeRemoteInterface = (RemoteInterface<MessageType>) nodeRegistry.lookup("RemoteInterface");
                remoteImplementation.remoteNodes.add(new RemoteNode<>(entry.getIpAddress(), entry.getPort(), nodeRemoteInterface));
                nodeRemoteInterface.addMeBack(remoteImplementation.hostname, remoteImplementation.port);
            }
            networkNodes.add(new Entity(hostname, port));
        }
        return networkNodes;
    }

    /**
     * This method is used to send a message to a specific node by using rmi
     * @param hostname the hostname of the remote node
     * @param port the port associated to the rmi registry in the remote node
     * @param message the message to send to the remote node
     * @throws RemoteNodeNotFound the remote node is not found, you must first connect to it
     * @throws RemoteException communication-related exception that may occur during remote calls
     * @throws NotBoundException the remote node has not bound its remote implementation
     * @throws NotInitialized this instance hasn't been initialized, you must do it first
     * @throws SnapshotInterruptException it's not possible to remove a node when a snapshot is running
     */
    public void sendMessage(String hostname, int port, MessageType message) throws RemoteNodeNotFound, RemoteException, NotBoundException, NotInitialized, SnapshotInterruptException {
        if (remoteImplementation.appConnector == null) throw new NotInitialized("You must initialize the instance and connect to hostname: " + hostname +
                "and port " + port +
                "before sending a message");

        synchronized (remoteImplementation) {
            getRemoteInterface(hostname, port).receiveMessage(remoteImplementation.hostname, remoteImplementation.port, message);
        }
    }

    /** This method is used to update the state. It makes a deep copy to store inside the remoteImplementation
     * @param state the object to save
     * @throws StateUpdateException something went wrong making a deep copy
     * */
    public void updateState(StateType state) throws StateUpdateException {
        synchronized (remoteImplementation.currentStateLock) {
            try {
                this.remoteImplementation.currentState=deepClone(state);
            } catch (IOException | ClassNotFoundException e) {
                throw new StateUpdateException("Problem in updating the state");
            }
        }
    }

    /**
     * This method is used to start a snapshot with the distributed snapshot algorithm
     * @throws RemoteException communication-related exception that may occur during remote calls
     * @throws DoubleMarkerException received multiple marker (same id) from the same link
     * @throws UnexpectedMarkerReceived the sender node is not present in the remote nodes list
     * @throws NotInitialized this instance hasn't been initialized, you must do it first
     * */
    public void initiateSnapshot() throws RemoteException, DoubleMarkerException, UnexpectedMarkerReceived, NotInitialized {
        if (remoteImplementation.appConnector == null) throw new NotInitialized("You must initialize the instance before starting a snapshot");

        int snapshotId;
        synchronized (remoteImplementation) {
            String snapshotIdString = remoteImplementation.hostname + remoteImplementation.port + remoteImplementation.localSnapshotCounter;
            snapshotId = snapshotIdString.hashCode();
            remoteImplementation.localSnapshotCounter++;
            Snapshot<StateType, MessageType> snap = new Snapshot<>(snapshotId, remoteImplementation.currentState, remoteImplementation.remoteNodes);
            remoteImplementation.runningSnapshots.add(snap);
        }
        // Assumption from the text: no change in the network topology is allowed during a snapshot!
        for (RemoteNode<MessageType> remoteNode : remoteImplementation.remoteNodes) {
            //TODO: remove SystemPrintln
            System.out.println(remoteImplementation.hostname + ":" + remoteImplementation.port + " | Sending MARKER to: " + remoteNode.hostname + ":" + remoteNode.port);
            remoteNode.remoteInterface.receiveMarker(remoteImplementation.hostname, remoteImplementation.port, remoteImplementation.hostname, remoteImplementation.port, snapshotId);
        }
    }

    /**
     * This is method is used to disconnect from a remote node
     * @param hostname the hostname of the entity that should be removed
     * @param port the RMI registry port of the entity that should be removed
     * @throws RemoteException communication-related exception that may occur during remote calls
     * @throws SnapshotInterruptException it's not possible to remove a node when a snapshot is running
     * @throws NotInitialized this instance hasn't been initialized, you must do it first
     * @throws OperationForbidden it is not possible to remove a connection while a snapshot is running
     * */
    public void removeConnection(String hostname, int port) throws OperationForbidden, SnapshotInterruptException, RemoteException, NotInitialized {
        if (remoteImplementation.appConnector == null) throw new NotInitialized("You must initialize the connection before trying to remove the node hostname +" +
                "");

        // Since no change in the network topology is allowed during a snapshot
        // this function WONT BE CALLED if any snapshot is running THIS IS AN ASSUMPTION FROM THE TEXT
        synchronized (remoteImplementation) {
            if (!remoteImplementation.runningSnapshots.isEmpty()) {
                throw new OperationForbidden("Unable to remove connection while snapshots are running");
            }
            RemoteNode<MessageType> remoteNode = remoteImplementation.getRemoteNode(hostname, port);
            remoteNode.remoteInterface.removeMe(remoteImplementation.hostname, remoteImplementation.port);
            remoteImplementation.remoteNodes.remove(remoteNode);
        }
    }

    /**
     *
     * */
    public void stop() {
        try {
            //TODO: remove the stop of the whole jvm
            UnicastRemoteObject.unexportObject(remoteImplementation, true);
            LocateRegistry.getRegistry(remoteImplementation.port).unbind("RemoteInterface");
            //SHOULD STOP HERE!!
            System.exit(0);
        } catch (Exception e) {
            //TODO: rimuovere printStackTrace
            //TODO: evitare generic exceptions
            e.printStackTrace();
        }
    }

    public void restoreLastSnapshot() throws RestoreAlreadyInProgress, RemoteException, NotBoundException {
        int snapshotToRestore=0; //TODO: get the correct id to restore

        // we set our node to the not-ready state and restore our connections and state according to our snapshot
        this.remoteImplementation.setReady(false);
        this.remoteImplementation.restoreConnections(snapshotToRestore);
        this.remoteImplementation.restoreState(snapshotToRestore);

        // we set all the nodes in our new connections list to the not-ready state and proceed to set their connection
        // list and state according to their snapshot
        for(RemoteNode<MessageType> remoteNode : this.remoteImplementation.remoteNodes) {
            remoteNode.remoteInterface.setReady(false);
            remoteNode.remoteInterface.restoreConnections(snapshotToRestore);
            remoteNode.remoteInterface.restoreState(snapshotToRestore);
        }

        // now all the nodes can be set to the ready state
        // TODO: what if the application is automated (like sending a message every X seconds)?
        //  in this case the application would start as soon as the ready state is set to true, without restoring
        //  the incoming messages
        this.remoteImplementation.setReady(true);
        for(RemoteNode<MessageType> remoteNode : this.remoteImplementation.remoteNodes) {
            remoteNode.remoteInterface.setReady(true);
        }
        //now all the "modifying" functions can be called again, hence we will start handling the old messages

        //handle old incoming messages
        //TODO: this should be handled in parallel
        this.remoteImplementation.restoreOldIncomingMessages(snapshotToRestore);
        for(RemoteNode<MessageType> remoteNode : this.remoteImplementation.remoteNodes) {
            remoteNode.remoteInterface.restoreOldIncomingMessages(snapshotToRestore);
        }
    }


    /**
     * This is method is used to get the reference of a RemoteNode
     * @param hostname the hostname of the remote node
     * @param port the RMI registry port of the remote node
     * @throws RemoteNodeNotFound the remote node is not found
     * */
    private RemoteInterface<MessageType> getRemoteInterface(String hostname, int port) throws RemoteNodeNotFound {
        int index= remoteImplementation.remoteNodes.indexOf(new RemoteNode<MessageType>(hostname,port,null));
        if(index==-1){
           throw new RemoteNodeNotFound("RemoteNode with the following hostname " + hostname +
                   " and port " +port+
                   " not found");
        }
        RemoteNode<MessageType> remoteNode = remoteImplementation.remoteNodes.get(index);
        return remoteNode.remoteInterface;
    }

    /**
     * Courtesy of
     * www.infoworld.com/article/2077578/java-tip-76--an-alternative-to-the-deep-copy-technique.html
     * given that we decided to make a deep copy of a serializable object this trick allows us
     * to make it by using only properties deriving from the fact that that object is serializable
     * (so no Cloneable or similar approaches)
     */
    private StateType deepClone(StateType state) throws IOException, ClassNotFoundException {
        // First serializing the object and its state to memory using
        // ByteArrayOutputStream instead of FileOutputStream.
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(state);

        // And then deserializing it from memory using ByteArrayOutputStream instead of FileInputStream.
        // Deserialization process will create a new object with the same state as in the serialized object,
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bis);
        return (StateType) in.readObject();
    }
}

/** Data class that encapsulates the structure of a Remote Node.
 * It contains the node identifiers (hostname and port) as well as the
 * reference to its corresponding Remote RMI Interface. The class is also
 * used to keep track of the Snapshot Received by the corresponding
 * Remote Node.
 * @param <MessageType> this is the type that will be exchanged as a message between nodes
 * */
class RemoteNode<MessageType> {

    /**
     * Hostname string associated to this entity
     * */
    protected String hostname;

    /**
     * Port number associated to this entity
     * */
    protected int port;

    /**
     * A reference to the Remote RMI Interface associated to this Remote Node
     * */
    protected RemoteInterface<MessageType> remoteInterface;

    /**
     * A List of Integers containing the IDs of the snapshots for which
     * the corresponding marker has been received by this Remote Node.
     * It is used to enable management of multiple, parallel, snapshots.
     * */
    protected ArrayList<Integer> snapshotIdsReceived = new ArrayList<>(); //holds the marker.id received from this remoteNode (for multiple concurrent distributed snapshots)

    /**
     * Constructor for the Remote Node object, it allows encapsulation of hostname
     * and port, and provides a reference to the Remote RMI Interface corresponding
     * to the created Remote Node
     * */
    public RemoteNode(String hostname, int port, RemoteInterface<MessageType> remoteInterface) {
        this.hostname = hostname;
        this.port = port;
        this.remoteInterface = remoteInterface;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteNode<MessageType> that = (RemoteNode<MessageType>) o;
        return port == that.port && Objects.equals(hostname, that.hostname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, port);
    }

}
