package library;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Objects;

public class Node extends RemoteImplementation {

    /**
     *
     * @param appConnector
     * @param ip_address
     * @param port
     */
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
            getRemoteNodes().add(new RemoteNode(ip_address,port,remoteInterface));

            //TODO: addMeBack
            //remoteInterface.addMeBack(InetAddress.getLocalHost().getHostAddress(), this.port);
        }
        catch (Exception e) {
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
            getRemoteInterface(ip_address, port).receiveMessage(this.getIp_address(), this.getPort(),message);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <StateType> void updateState(StateType state){
        //TODO: save current state to a variable (probably variable in the remoteImplementation)
    }

    public void initiateSnapshot(){
        int snapshotId=1;
        this.getRunningSnapshotIds().add(snapshotId);

        for (RemoteNode remoteNode : this.getRemoteNodes()){
            try{
                System.out.println(this.getIp_address() + ":" + this.getPort() + " | Sending MARKER to: "+remoteNode.getIp_address()+":"+remoteNode.getPort());
                //TODO: come si decide ID del marker? numero randomico grosso? dovrebbero fare agree sul successivo markerId, ma non credo sia necessario
                remoteNode.getSnapshotIdsSent().add(snapshotId);
                remoteNode.getRemoteInterface().receiveMarker(this.getIp_address(), this.getPort(), this.getIp_address(), this.getPort(), 1);
            }catch (Exception e){
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
        int index= getRemoteNodes().indexOf(new RemoteNode(ip_address,port,null));
        return getRemoteNodes().get(index).getRemoteInterface();
    }

}

class RemoteNode {
    String ip_address;
    int port;
    RemoteInterface remoteInterface; //the remote interface of the node
    ArrayList<Integer> snapshotIdsReceived = new ArrayList<>(); //holds the marker.id received from this remoteNode (for multiple concurrent distributed snapshots)
    ArrayList<Integer> snapshotIdsSent = new ArrayList<>(); //holds the marker.id sent to this remoteNode (for multiple concurrent distributed snapshots)

    public RemoteNode(String ip_address, int port, RemoteInterface remoteInterface) {
        this.ip_address = ip_address;
        this.port = port;
        this.remoteInterface = remoteInterface;
    }

    public ArrayList<Integer> getSnapshotIdsSent() {
        return snapshotIdsSent;
    }
    public String getIp_address() {
        return ip_address;
    }
    public int getPort() {
        return port;
    }
    public ArrayList<Integer> getSnapshotIdsReceived() {
        return snapshotIdsReceived;
    }
    public RemoteInterface getRemoteInterface() {
        return remoteInterface;
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
