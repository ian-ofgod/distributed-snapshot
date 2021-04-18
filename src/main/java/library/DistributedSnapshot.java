package library;

import library.exceptions.*;

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
    protected RemoteImplementation<StateType,MessageType> remoteImplementation = new RemoteImplementation<>();

    /**
     *
     * */
    public DistributedSnapshot() {}

    /**
     *
     * */
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
    public void init(String yourHostname, int rmiRegistryPort, AppConnector<MessageType> appConnector) throws RemoteException, AlreadyBoundException {
        remoteImplementation.hostname =yourHostname;
        remoteImplementation.port=rmiRegistryPort;
        remoteImplementation.appConnector=appConnector;

        RemoteInterface<MessageType> stub = (RemoteInterface<MessageType>) UnicastRemoteObject.exportObject(remoteImplementation, 0);
        Registry registry = LocateRegistry.createRegistry(remoteImplementation.port);
        registry.bind("RemoteInterface", stub);
    }

    /**
     * This method adds a new link. To do so, it looks up the registry to the given ip and port and saves the reference.
     * This methods is not exposed in the rmi registry to avoid it being invoked by external entities.
     * @param hostname the ip address of the host running the rmi registry
     * @param port the port where the rmi registry is running
     */
    public void addConnection(String hostname, int port) throws RemoteException, NotBoundException, RemoteNodeAlreadyPresent {
        if (!remoteImplementation.remoteNodes.contains(new RemoteNode<MessageType>(hostname,port,null))) {
                Registry registry = LocateRegistry.getRegistry(hostname, port);
                RemoteInterface<MessageType> remoteInterface = (RemoteInterface<MessageType>) registry.lookup("RemoteInterface");
                remoteImplementation.remoteNodes.add(new RemoteNode<>(hostname, port, remoteInterface));
                remoteInterface.addMeBack(remoteImplementation.hostname, remoteImplementation.port);
        } else {
            throw new RemoteNodeAlreadyPresent();
        }
    }

    /**
     * This method is used to send a message to a specific node by using rmi
     * @param hostname the ip address of the remote node
     * @param port the port associated to the rmi registry in the remote node
     * @param message the message to send to the remote node
     */
    public void sendMessage(String hostname, int port, MessageType message) throws RemoteNodeNotFound, RemoteException, NotBoundException, SnapshotInterruptException {
        getRemoteInterface(hostname, port).receiveMessage(remoteImplementation.hostname, remoteImplementation.port, message);
    }

    /**
     *
     * */
    public void updateState(StateType state) {
        synchronized (remoteImplementation.currentStateLock) {
            this.remoteImplementation.current_state=state;
        }
    }

    /**
     *
     * */
    public void initiateSnapshot() throws RemoteException, DoubleMarkerException, UnexpectedMarkerReceived {
        String snapshotIdString= remoteImplementation.hostname + remoteImplementation.port + remoteImplementation.localSnapshotCounter;
        int snapshotId = snapshotIdString.hashCode();
        remoteImplementation.localSnapshotCounter++;
        Snapshot<StateType, MessageType> snap = new Snapshot<>(snapshotId, remoteImplementation.current_state);
        remoteImplementation.runningSnapshots.add(snap);

        for (RemoteNode<MessageType> remoteNode : remoteImplementation.remoteNodes){
            System.out.println(remoteImplementation.hostname + ":" + remoteImplementation.port + " | Sending MARKER to: "+remoteNode.hostname +":"+remoteNode.port);
            remoteNode.remoteInterface.receiveMarker(remoteImplementation.hostname, remoteImplementation.port, remoteImplementation.hostname, remoteImplementation.port, snapshotId);
        }
    }

    /**
     *
     * */
    public void removeConnection(String hostname, int port) throws OperationForbidden, SnapshotInterruptException, RemoteException {
        //since no change in the network topology is allowed during a snapshot
        //this function WONT BE CALLED if any snapshot is running THIS IS AN ASSUMPTION FROM THE TEXT
        if (!remoteImplementation.runningSnapshots.isEmpty()) {
            throw new OperationForbidden("Unable to remove connection while snapshots are running");
        }
        RemoteNode<MessageType> remoteNode = remoteImplementation.getRemoteNode(hostname,port);
        remoteNode.remoteInterface.removeMe(remoteImplementation.hostname, remoteImplementation.port);
        remoteImplementation.remoteNodes.remove(remoteNode);
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
