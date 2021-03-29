package library;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;


public class RemoteImplementation implements RemoteInterface {
    //info on the current node
    protected String ip_address;
    protected int port;

    //store remote references to the linked nodes
    private ArrayList<RemoteNode> remoteNodes = new ArrayList<>();

    //this is the provided implementation of the class Observer
    private AppConnector appConnector;

    //list of the ids of running snapshots
    private ArrayList<Integer> runningSnapshotIds = new ArrayList<>();


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

        if (runningSnapshotIds.isEmpty()) { //no marker received
            //TODO: normal passing of the messages to the real application with the observer pattern
            appConnector.handleIncomingMessage(senderIp, senderPort,message);
        } else if (!runningSnapshotIds.isEmpty()) { //snapshot running, marker received
            //TODO: we are running a snapshot and the current node has already received the marker so we have to save the current received message including the sender
                /*
                    1) get who sent the message
                    2) add the message and the sender to the local snapshot
                 */
            appConnector.handleIncomingMessage(senderIp, senderPort, message);
        }
    }


    @Override
    public void addMeBack(String ip_address, int port) throws RemoteException{
        //TODO: NOT WORKING
       //forse funziona ho sistemato il fatto che non faceva throws correttamente, ma lascio commentato per ora

       /* try {
            Registry registry = LocateRegistry.getRegistry(ip_address, port);
            RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
            remoteNodes.add(new RemoteNode(ip_address, port, remoteInterface));
        } catch (Exception e) {
            e.printStackTrace();
        }*/

    }


    AppConnector getObserver() {
        return appConnector;
    }

    void setAppConnector(AppConnector o) {
        this.appConnector = o;
    }


    private void recordSnapshotId(String senderIp, int senderPort, int snapshotId) {
        RemoteNode remoteNode = getRemoteNode(senderIp,senderPort);
        if(remoteNode!=null) {
            if(remoteNode.getSnapshotIdsReceived().contains(snapshotId)){
                System.out.println(ip_address+":"+port + " | ERROR: received multiple marker (same id) for the same link");
            }else {
                System.out.println(ip_address+":"+port + " | Added markerId for the remote node who called");
                remoteNode.getSnapshotIdsReceived().add(snapshotId);
            }
        }
    }

    //send the marker to all connected nodes
    private void propagateMarker(String initiatorIp, int initiatorPort, int snapshotId) {
        for (RemoteNode remoteNode : remoteNodes) {
            try {
                if (!remoteNode.getSnapshotIdsSent().contains(snapshotId)) {
                    System.out.println(ip_address+":"+port + " | Sending marker");
                    remoteNode.getSnapshotIdsSent().add(snapshotId);
                    remoteNode.getRemoteInterface().receiveMarker(this.ip_address, this.port, initiatorIp, initiatorPort, snapshotId);
                } else {
                    System.out.println(ip_address+":"+port + " | Received a marker from a node where i have already sent the marker");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private RemoteNode getRemoteNode(String ip_address, int port){
        for (RemoteNode remoteNode : remoteNodes) {
            if(remoteNode.getIp_address().equals(ip_address) && remoteNode.getPort()==port)
                return remoteNode;
        }
        return null;
    }

    //check if we have received marker from all the links
    private boolean receivedMarkerFromAllLinks(int snapshotId){
        int numOfLinks = remoteNodes.size();
        for (RemoteNode remoteNode : remoteNodes) {
            if (remoteNode.getSnapshotIdsReceived().contains(snapshotId)) {
                numOfLinks--;
            }
        }
        if (numOfLinks == 0) { //we have received a marker from all the channels
            System.out.println(ip_address + ":" + port + " | Received marker from all the channel");
            return true;
        }
        return false;
    }

    public String getIp_address() {
        return ip_address;
    }

    public int getPort() {
        return port;
    }

    public ArrayList<RemoteNode> getRemoteNodes() {
        return remoteNodes;
    }

    public AppConnector getAppConnector() {
        return appConnector;
    }

    public ArrayList<Integer> getRunningSnapshotIds() {
        return runningSnapshotIds;
    }


}