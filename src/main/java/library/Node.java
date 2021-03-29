package library;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Objects;

public class Node extends RemoteImplementation {
    public Node(AppConnector appConnector, String ip_address, int port){
        setAppConnector(appConnector);
        this.port=port;
        this.ip_address=ip_address;

        try {
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(this, 0);
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind("RemoteInterface", stub);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * This method adds a new link. To do so, it looks up the registry to the given ip and port and saves the reference.
     * This methods is not exposed in the rmiregistry to avoid it beeing invoked by external entities.
     * @param ip_address the ip address of the host running the rmiregistry
     * @param port the port where the rmi registry is running
     */
    public void addConnection(String ip_address, int port) {
        try {
            Registry registry = LocateRegistry.getRegistry(ip_address, port);
            RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
            this.remoteNodes.add(new RemoteNode(ip_address,port,remoteInterface));
            remoteInterface.addMeBack(this.ip_address, this.port);
        }
        catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }


    /**
     * This method is used to send a message to a specific node by using rmi
     * @param ip_address the ip address of the remote node
     * @param port the port associated to the rmi registry in the remote node
     * @param message the message to send to the remote node
     * @param <MessageType> the message type to send
     */
    public <MessageType> void sendMessage(String ip_address, int port, MessageType message){
        try {
            getRemoteInterface(ip_address, port).receiveMessage(this.ip_address, this.port,message);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public <StateType> void updateState(StateType state){
        //TODO: save current state to a variable (probably variable in the remoteImplementation)
    }

    public void initiateSnapshot(){
        int snapshotId=1;
        this.runningSnapshotIds.add(snapshotId);

        for (RemoteNode remoteNode : this.remoteNodes){
            try{
                System.out.println(this.ip_address + ":" + this.port + " | Sending MARKER to: "+remoteNode.ip_address+":"+remoteNode.port);
                //TODO: come si decide ID del marker? numero randomico grosso? dovrebbero fare agree sul successivo markerId, ma non credo sia necessario
                //remoteNode.getSnapshotIdsSent().add(snapshotId);
                remoteNode.remoteInterface.receiveMarker(this.ip_address, this.port, this.ip_address, this.port, 1);
            }catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    //TODO: method remove link
    
    //TODO: method stop library (rmiRegistry)


    /*
        COMMODITY FUNCTIONS
    */

    private RemoteInterface getRemoteInterface(String ip_address, int port){
        int index= this.remoteNodes.indexOf(new RemoteNode(ip_address,port,null));
        return this.remoteNodes.get(index).remoteInterface;
    }

}

class RemoteNode {
    protected String ip_address;
    protected int port;
    protected RemoteInterface remoteInterface; //the remote interface of the node
    protected ArrayList<Integer> snapshotIdsReceived = new ArrayList<>(); //holds the marker.id received from this remoteNode (for multiple concurrent distributed snapshots)

    public RemoteNode(String ip_address, int port, RemoteInterface remoteInterface) {
        this.ip_address = ip_address;
        this.port = port;
        this.remoteInterface = remoteInterface;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteNode that = (RemoteNode) o;
        return port == that.port && Objects.equals(ip_address, that.ip_address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip_address, port);
    }


}
