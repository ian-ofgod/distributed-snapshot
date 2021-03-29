package library;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Optional;


class RemoteImplementation implements RemoteInterface {
    //info on the current node
    protected String ip_address;
    protected int port;

    //store remote references to the linked nodes
    protected ArrayList<RemoteNode> remoteNodes = new ArrayList<>();

    //this is the provided implementation of the class Observer
    protected AppConnector appConnector;

    //list of the ids of running snapshots
    protected ArrayList<Integer> runningSnapshotIds = new ArrayList<>();


    //TODO: separare in due funzioni markerMessage e receiveMessage
    @Override
    public void receiveMarker(String senderIp, int senderPort, String initiatorIp, int initiatorPort, int snapshotId) throws RemoteException {

        System.out.println(ip_address + ":" + port + " | RECEIVED MARKER from: "+senderIp+":"+senderPort);
        if (!runningSnapshotIds.contains(snapshotId)) {
            System.out.println(ip_address + ":" + port + " | First time receiving a marker");
            runningSnapshotIds.add(snapshotId);
            recordSnapshotId(senderIp, senderPort, snapshotId);
            propagateMarker(initiatorIp, initiatorPort, snapshotId);
        }else{
            recordSnapshotId(senderIp, senderPort, snapshotId);
        }

        if (receivedMarkerFromAllLinks(snapshotId)) { //we have received a marker from all the channels
            runningSnapshotIds.remove(Integer.valueOf(snapshotId));
        }

    }

    @Override
    public <MessageType> void receiveMessage(String senderIp, int senderPort, MessageType message) throws RemoteException {
        //for debug purposes
        System.out.println(ip_address + ":" + port + " | Received a message from remoteNode: " + senderIp + ":" + senderPort);

        if (!runningSnapshotIds.isEmpty()) { //snapshot running, marker received
            //TODO: save message into all the running snapshots
        }
        appConnector.handleIncomingMessage(senderIp, senderPort, message);

    }


    @Override
    public void addMeBack(String ip_address, int port) throws RemoteException{
       try {
            Registry registry = LocateRegistry.getRegistry(ip_address, port);
            RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
            remoteNodes.add(new RemoteNode(ip_address, port, remoteInterface));
            appConnector.handleNewConnection(ip_address,port);
        } catch (RemoteException | NotBoundException e) {
           e.printStackTrace();
       }
    }


    @Override
    public void removeMe(String ip_address, int port) throws RemoteException {
        if(!this.runningSnapshotIds.isEmpty()) {
            System.out.println(ip_address+":"+port + " | ERROR: REMOVING DURING SNAPSHOT, ASSUMPTION NOT RESPECTED");
        }
        RemoteNode remoteNode = getRemoteNode(ip_address,port);
        this.remoteNodes.remove(remoteNode);
    }

    private void recordSnapshotId(String senderIp, int senderPort, int snapshotId) {
        RemoteNode remoteNode = getRemoteNode(senderIp,senderPort);
        if(remoteNode!=null) {
            if(remoteNode.snapshotIdsReceived.contains(snapshotId)){
                System.out.println(ip_address+":"+port + " | ERROR: received multiple marker (same id) for the same link");
            }else {
                System.out.println(ip_address+":"+port + " | Added markerId for the remote node who called");
                remoteNode.snapshotIdsReceived.add(snapshotId);
            }
        }
    }

    //send the marker to all connected nodes
    private void propagateMarker(String initiatorIp, int initiatorPort, int snapshotId) {
        for (RemoteNode remoteNode : remoteNodes) {
            try {
                remoteNode.remoteInterface.receiveMarker(this.ip_address, this.port, initiatorIp, initiatorPort, snapshotId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    protected RemoteNode getRemoteNode(String ip_address, int port){
        for (RemoteNode remoteNode : remoteNodes) {
            if(remoteNode.ip_address.equals(ip_address) && remoteNode.port==port)
                return remoteNode;
        }
        return null;
    }

    //check if we have received marker from all the links
    private boolean receivedMarkerFromAllLinks(int snapshotId){
        int numOfLinks = remoteNodes.size();
        for (RemoteNode remoteNode : remoteNodes) {
            if (remoteNode.snapshotIdsReceived.contains(snapshotId)) {
                numOfLinks--;
            }
        }
        if (numOfLinks == 0) { //we have received a marker from all the channels
            System.out.println(ip_address + ":" + port + " | Received marker from all the channel");
            return true;
        }
        return false;
    }

    void setAppConnector(AppConnector o) {
        this.appConnector = o;
    }
}