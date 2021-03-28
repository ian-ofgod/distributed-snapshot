package library;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class Node extends RemoteImplementation {

     /**
     * This method starts the registry in the current host and bind the methods specified in the RemoteInterface to it.
     * It also populate the ip_address with the external ip address of the current host.
     * @param port the port that will be associated to the rmi registry
     */
    public Node(AppConnector appConnector, String ip_address, int port, int id){
        setAppConnector(appConnector);
        this.id=id;
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
            remoteNodes.add(new RemoteNode(ip_address,port,remoteInterface));

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
            getRemoteInterface(ip_address, port).receiveMessage(this.ip_address, this.port,message);
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
        runningSnapshotIds.add(snapshotId);

        for (RemoteNode remoteNode : remoteNodes){
            try{
                System.out.println(ip_address + ":" + port + " | Sending MARKER to: "+remoteNode.getIp_address()+":"+remoteNode.getPort());
                //TODO: come si decide ID del marker? numero randomico grosso? dovrebbero fare agree sul successivo markerId, ma non credo sia necessario
                remoteNode.getSnapshotIdsSent().add(snapshotId);
                remoteNode.getRemoteInterface().receiveMarker(this.ip_address, this.port, this.ip_address, this.port, 1);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    //TODO: method remove link
    
    //TODO: method stop library (rmiRegistry)

    public static void main(String[] args) throws RemoteException, UnknownHostException {
        BasicApp1 basicApp1 = new BasicApp1();
        BasicApp2 basicApp2 = new BasicApp2();

        Node node1 = new Node(basicApp1, InetAddress.getLocalHost().getHostAddress(), 1111,1);
        Node node2 = new Node(basicApp2,InetAddress.getLocalHost().getHostAddress(), 1112,2 );

        node1.addConnection(InetAddress.getLocalHost().getHostAddress(), 1112);
        node2.addConnection(InetAddress.getLocalHost().getHostAddress(), 1111);

        node1.sendMessage(InetAddress.getLocalHost().getHostAddress(), 1112, new Message("Messaggio 1->2 che è stato processato da 2"));
        node2.sendMessage(InetAddress.getLocalHost().getHostAddress(), 1111, new Message("Messaggio 2->1 che è stato processato da 1"));

        node1.initiateSnapshot();
    }


    /*
        COMMODITY FUNCTIONS
    */

    private RemoteInterface getRemoteInterface(String ip_address, int port){
        int index= remoteNodes.indexOf(new RemoteNode(ip_address,port,null));
        return remoteNodes.get(index).getRemoteInterface();
    }

}