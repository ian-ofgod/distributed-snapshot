package library;

import library.exceptions.DoubleMarkerException;
import library.exceptions.OperationForbidden;
import library.exceptions.RemoteNodeNotFound;
import library.exceptions.SnapshotInterruptException;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Objects;

//TODO: change to static (only at the end of prj) ; correct trhrow of RemoteException
/**
 *
 * */
public class Node {
    /**
     *
     * */
    protected static RemoteImplementation remoteImplementation = new RemoteImplementation();

    /**
     *
     * */
    public Node() {}

    /**
     *
     * */
    public Node(AppConnector appConnector, String ipAddress, int port){
        remoteImplementation.setAppConnector(appConnector);
        remoteImplementation.port=port;
        remoteImplementation.ipAddress =ipAddress;

        try {
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(remoteImplementation, 0);
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind("RemoteInterface", stub);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     *
     * */
    public static void init(String yourIp, int rmiRegistryPort,AppConnector appConnector){
        remoteImplementation.ipAddress =yourIp;
        remoteImplementation.port=rmiRegistryPort;
        remoteImplementation.appConnector=appConnector;

        try {
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(remoteImplementation, 0);
            Registry registry = LocateRegistry.createRegistry(remoteImplementation.port);
            registry.bind("RemoteInterface", stub);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method adds a new link. To do so, it looks up the registry to the given ip and port and saves the reference.
     * This methods is not exposed in the rmiregistry to avoid it beeing invoked by external entities.
     * @param ipAddress the ip address of the host running the rmiregistry
     * @param port the port where the rmi registry is running
     */
    public static void addConnection(String ipAddress, int port) {
        if(!remoteImplementation.remoteNodes.contains(new RemoteNode(ipAddress,port,null))) {
            try {
                Registry registry = LocateRegistry.getRegistry(ipAddress, port);
                RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
                remoteImplementation.remoteNodes.add(new RemoteNode(ipAddress, port, remoteInterface));
                remoteInterface.addMeBack(remoteImplementation.ipAddress, remoteImplementation.port);
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }
        else{
            //TODO: throw RemoteNodeAlreadyPresent or just ignore the call?
        }
    }

    /**
     * This method is used to send a message to a specific node by using rmi
     * @param ipAddress the ip address of the remote node
     * @param port the port associated to the rmi registry in the remote node
     * @param message the message to send to the remote node
     * @param <MessageType> the message type to send
     */
    public static <MessageType> void sendMessage(String ipAddress, int port, MessageType message) throws RemoteNodeNotFound {
        try {
            getRemoteInterface(ipAddress, port).receiveMessage(remoteImplementation.ipAddress, remoteImplementation.port, message);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * */
    public static <StateType> void updateState(StateType state){
        //TODO: save current state to a variable (probably variable in the remoteImplementation)
    }

    /**
     *
     * */
    public static void initiateSnapshot(){
        //TODO: how to decide the snapshot id so that it does not conflicts with the others?
        int snapshotId=1;

        Snapshot snap = new Snapshot(snapshotId);
        remoteImplementation.runningSnapshots.add(snap);

        for (Object rn : remoteImplementation.remoteNodes){
            RemoteNode remoteNode = (RemoteNode) rn;
            try{
                System.out.println(remoteImplementation.ipAddress + ":" + remoteImplementation.port + " | Sending MARKER to: "+remoteNode.ipAddress+":"+remoteNode.port);
                remoteNode.remoteInterface.receiveMarker(remoteImplementation.ipAddress, remoteImplementation.port, remoteImplementation.ipAddress, remoteImplementation.port, 1);
            }catch (RemoteException | DoubleMarkerException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *
     * */
    public static void removeConnection(String ipAddress, int port) throws OperationForbidden {
        //since no change in the network topology is allowed during a snapshot
        //this function WONT BE CALLED if any snapshot is running THIS IS AN ASSUMPTION FROM THE TEXT
        if(!remoteImplementation.runningSnapshots.isEmpty()) {
            throw new OperationForbidden("Unable to remove connection while snapshots are running");
        }

        RemoteNode remoteNode = remoteImplementation.getRemoteNode(ipAddress,port);
        try {
            remoteNode.remoteInterface.removeMe(remoteImplementation.ipAddress, remoteImplementation.port);
        }catch (RemoteException | SnapshotInterruptException e){
            e.printStackTrace();
        }
        remoteImplementation.remoteNodes.remove(remoteNode);
    }

    /**
     *
     * */
    public static void stop() {
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
    private static RemoteInterface getRemoteInterface(String ipAddress, int port) throws RemoteNodeNotFound {
        int index= remoteImplementation.remoteNodes.indexOf(new RemoteNode(ipAddress,port,null));
        if(index==-1){ // RemoteNode with the specified ipAddress and port not found!
           throw new RemoteNodeNotFound();
        }
        RemoteNode remoteNode = (RemoteNode) remoteImplementation.remoteNodes.get(index);
        return remoteNode.remoteInterface;
    }

}

/**
 *
 * */
class RemoteNode {

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
    protected RemoteInterface remoteInterface; //the remote interface of the node

    /**
     *
     * */
    protected ArrayList<Integer> snapshotIdsReceived = new ArrayList<>(); //holds the marker.id received from this remoteNode (for multiple concurrent distributed snapshots)

    /**
     *
     * */
    public RemoteNode(String ipAddress, int port, RemoteInterface remoteInterface) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.remoteInterface = remoteInterface;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteNode that = (RemoteNode) o;
        return port == that.port && Objects.equals(ipAddress, that.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress, port);
    }


}
