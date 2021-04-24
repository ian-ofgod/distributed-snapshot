package library;

import library.exceptions.*;

import javax.swing.plaf.nimbus.State;
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
 *
 * */
public class DistributedSnapshot<StateType, MessageType> {
    /**
     *
     * */
    protected final RemoteImplementation<StateType,MessageType> remoteImplementation = new RemoteImplementation<>();

    /**
     *
     * */
    public DistributedSnapshot() {}

    //TODO: remove
    public DistributedSnapshot(AppConnector<MessageType> appConnector, String hostname, int port){
        remoteImplementation.appConnector=appConnector;
        remoteImplementation.port=port;
        remoteImplementation.hostname =hostname;

        try {
            RemoteInterface<MessageType> stub = (RemoteInterface<MessageType>) UnicastRemoteObject.exportObject(remoteImplementation, 0);
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind("RemoteInterface", stub);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    public void init(String yourHostname, int rmiRegistryPort, AppConnector<MessageType> appConnector) throws RemoteException, AlreadyBoundException, AlreadyInitialized {
        if (remoteImplementation.appConnector != null) throw new AlreadyInitialized();

        synchronized (remoteImplementation) {
            remoteImplementation.hostname = yourHostname;
            remoteImplementation.port = rmiRegistryPort;
            remoteImplementation.appConnector = appConnector;

            RemoteInterface<MessageType> stub = (RemoteInterface<MessageType>) UnicastRemoteObject.exportObject(remoteImplementation, 0);
            Registry registry = LocateRegistry.createRegistry(remoteImplementation.port);
            registry.bind("RemoteInterface", stub);
        }
    }

    /**
     * This method adds a new link. To do so, it looks up the registry to the given ip and port and saves the reference.
     * This methods is not exposed in the rmi registry to avoid it being invoked by external entities.
     * @param hostname the ip address of the host running the rmi registry
     * @param port the port where the rmi registry is running
     */
    public void addConnection(String hostname, int port) throws RemoteException, NotBoundException, RemoteNodeAlreadyPresent, NotInitialized {
        if (remoteImplementation.appConnector == null) throw new NotInitialized();

        synchronized (remoteImplementation) {
            if (!remoteImplementation.remoteNodes.contains(new RemoteNode<MessageType>(hostname, port, null))) {
                Registry registry = LocateRegistry.getRegistry(hostname, port);
                RemoteInterface<MessageType> remoteInterface = (RemoteInterface<MessageType>) registry.lookup("RemoteInterface");
                remoteImplementation.remoteNodes.add(new RemoteNode<>(hostname, port, remoteInterface));
                remoteInterface.addMeBack(remoteImplementation.hostname, remoteImplementation.port);
            } else {
                throw new RemoteNodeAlreadyPresent();
            }
        }
    }

    /**
     * This method is used to send a message to a specific node by using rmi
     * @param hostname the ip address of the remote node
     * @param port the port associated to the rmi registry in the remote node
     * @param message the message to send to the remote node
     */
    public void sendMessage(String hostname, int port, MessageType message) throws RemoteNodeNotFound, RemoteException, NotBoundException, SnapshotInterruptException, NotInitialized {
        if (remoteImplementation.appConnector == null) throw new NotInitialized();

        synchronized (remoteImplementation) {
            getRemoteInterface(hostname, port).receiveMessage(remoteImplementation.hostname, remoteImplementation.port, message);
        }
    }

    /**
     *
     * */
    public void updateState(StateType state) {
        synchronized (remoteImplementation.currentStateLock) {
            try {
                this.remoteImplementation.currentState=deepClone(state);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *
     * */
    public void initiateSnapshot() throws RemoteException, DoubleMarkerException, UnexpectedMarkerReceived, NotInitialized {
        if (remoteImplementation.appConnector == null) throw new NotInitialized();

        synchronized (remoteImplementation) {
            String snapshotIdString = remoteImplementation.hostname + remoteImplementation.port + remoteImplementation.localSnapshotCounter;
            int snapshotId = snapshotIdString.hashCode();
            remoteImplementation.localSnapshotCounter++;
            Snapshot<StateType, MessageType> snap = new Snapshot<>(snapshotId, remoteImplementation.currentState);
            remoteImplementation.runningSnapshots.add(snap);

            for (RemoteNode<MessageType> remoteNode : remoteImplementation.remoteNodes) {
                System.out.println(remoteImplementation.hostname + ":" + remoteImplementation.port + " | Sending MARKER to: " + remoteNode.hostname + ":" + remoteNode.port);
                remoteNode.remoteInterface.receiveMarker(remoteImplementation.hostname, remoteImplementation.port, remoteImplementation.hostname, remoteImplementation.port, snapshotId);
            }
        }
    }

    /**
     *
     * */
    public void removeConnection(String hostname, int port) throws OperationForbidden, SnapshotInterruptException, RemoteException, NotInitialized {
        if (remoteImplementation.appConnector == null) throw new NotInitialized();

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
            e.printStackTrace();
        }
    }


    /**
     *
     * */
    private RemoteInterface<MessageType> getRemoteInterface(String hostname, int port) throws RemoteNodeNotFound {
        int index= remoteImplementation.remoteNodes.indexOf(new RemoteNode<MessageType>(hostname,port,null));
        if(index==-1){ // RemoteNode with the specified hostname and port not found!
           throw new RemoteNodeNotFound();
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

/**
 *
 * */
class RemoteNode<MessageType> {

    /**
     *
     * */
    protected String hostname;

    /**
     *
     * */
    protected int port;

    /**
     *
     * */
    protected RemoteInterface<MessageType> remoteInterface; //the remote interface of the node

    /**
     *
     * */
    protected ArrayList<Integer> snapshotIdsReceived = new ArrayList<>(); //holds the marker.id received from this remoteNode (for multiple concurrent distributed snapshots)

    /**
     *
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
