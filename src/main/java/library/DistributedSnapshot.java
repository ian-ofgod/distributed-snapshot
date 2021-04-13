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
    public DistributedSnapshot(AppConnector<MessageType> appConnector, String ipAddress, int port){
        remoteImplementation.appConnector=appConnector;
        remoteImplementation.port=port;
        remoteImplementation.ipAddress =ipAddress;

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
    public void init(String yourIp, int rmiRegistryPort, AppConnector<MessageType> appConnector) throws RemoteException, AlreadyBoundException {
        remoteImplementation.ipAddress =yourIp;
        remoteImplementation.port=rmiRegistryPort;
        remoteImplementation.appConnector=appConnector;

        RemoteInterface<MessageType> stub = (RemoteInterface<MessageType>) UnicastRemoteObject.exportObject(remoteImplementation, 0);
        Registry registry = LocateRegistry.createRegistry(remoteImplementation.port);
        registry.bind("RemoteInterface", stub);
    }

    /**
     * This method adds a new link. To do so, it looks up the registry to the given ip and port and saves the reference.
     * This methods is not exposed in the rmi registry to avoid it being invoked by external entities.
     * @param ipAddress the ip address of the host running the rmi registry
     * @param port the port where the rmi registry is running
     */
    public void addConnection(String ipAddress, int port) throws RemoteException, NotBoundException, RemoteNodeAlreadyPresent {
        if (!remoteImplementation.remoteNodes.contains(new RemoteNode<MessageType>(ipAddress,port,null))) {
                Registry registry = LocateRegistry.getRegistry(ipAddress, port);
                RemoteInterface<MessageType> remoteInterface = (RemoteInterface<MessageType>) registry.lookup("RemoteInterface");
                remoteImplementation.remoteNodes.add(new RemoteNode<>(ipAddress, port, remoteInterface));
                remoteInterface.addMeBack(remoteImplementation.ipAddress, remoteImplementation.port);
        } else {
            throw new RemoteNodeAlreadyPresent();
        }
    }

    /**
     * This method is used to send a message to a specific node by using rmi
     * @param ipAddress the ip address of the remote node
     * @param port the port associated to the rmi registry in the remote node
     * @param message the message to send to the remote node
     */
    public void sendMessage(String ipAddress, int port, MessageType message) throws RemoteNodeNotFound, RemoteException, NotBoundException, SnapshotInterruptException {
        getRemoteInterface(ipAddress, port).receiveMessage(remoteImplementation.ipAddress, remoteImplementation.port, message);
    }

    /**
     *
     * */
    public void updateState(StateType state){
        this.remoteImplementation.current_state=state;
    }

    /**
     *
     * */
    public void initiateSnapshot() throws RemoteException, DoubleMarkerException {
        String snapshotIdString= remoteImplementation.ipAddress + remoteImplementation.port + remoteImplementation.localSnapshotCounter;
        int snapshotId = snapshotIdString.hashCode();
        remoteImplementation.localSnapshotCounter++;
        Snapshot<StateType, MessageType> snap = new Snapshot<>(snapshotId);
        remoteImplementation.runningSnapshots.add(snap);

        for (RemoteNode<MessageType> remoteNode : remoteImplementation.remoteNodes){
            System.out.println(remoteImplementation.ipAddress + ":" + remoteImplementation.port + " | Sending MARKER to: "+remoteNode.ipAddress+":"+remoteNode.port);
            remoteNode.remoteInterface.receiveMarker(remoteImplementation.ipAddress, remoteImplementation.port, remoteImplementation.ipAddress, remoteImplementation.port, snapshotId);
        }
    }

    /**
     *
     * */
    public void removeConnection(String ipAddress, int port) throws OperationForbidden, SnapshotInterruptException, RemoteException {
        //since no change in the network topology is allowed during a snapshot
        //this function WONT BE CALLED if any snapshot is running THIS IS AN ASSUMPTION FROM THE TEXT
        if (!remoteImplementation.runningSnapshots.isEmpty()) {
            throw new OperationForbidden("Unable to remove connection while snapshots are running");
        }
        RemoteNode<MessageType> remoteNode = remoteImplementation.getRemoteNode(ipAddress,port);
        remoteNode.remoteInterface.removeMe(remoteImplementation.ipAddress, remoteImplementation.port);
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
    private RemoteInterface<MessageType> getRemoteInterface(String ipAddress, int port) throws RemoteNodeNotFound {
        int index= remoteImplementation.remoteNodes.indexOf(new RemoteNode<MessageType>(ipAddress,port,null));
        if(index==-1){ // RemoteNode with the specified ipAddress and port not found!
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
    protected String ipAddress;

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
    public RemoteNode(String ipAddress, int port, RemoteInterface<MessageType> remoteInterface) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.remoteInterface = remoteInterface;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteNode<MessageType> that = (RemoteNode<MessageType>) o;
        return port == that.port && Objects.equals(ipAddress, that.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress, port);
    }


}
