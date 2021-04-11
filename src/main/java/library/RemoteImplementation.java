package library;

import library.exceptions.DoubleMarkerException;
import library.exceptions.SnapshotInterruptException;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;

/**
 *
 * */
//TODO: add MessageType as a generic type and handle all the Snapshot invocation with <StateType, MessageType>
class RemoteImplementation<StateType>  implements RemoteInterface {

    /**
     *
     * */
    protected String ipAddress;

    /**
     *
     * */
    protected int port;


    /**
     * Stores the current state: will fill the different Snapshot objects when created
     * */
    StateType current_state;

    //store remote references to the linked nodes
    /**
     *
     * */
    protected ArrayList<RemoteNode> remoteNodes = new ArrayList<>();

    //this is the provided implementation of the class Observer
    /**
     *
     * */
    protected AppConnector appConnector;

    //list of the ids of running snapshots
    /**
     *
     * */
    protected ArrayList<Snapshot> runningSnapshots = new ArrayList<>();



    @Override
    public void receiveMarker(String senderIp, int senderPort, String initiatorIp, int initiatorPort, int snapshotId) throws RemoteException, DoubleMarkerException {

        System.out.println(ipAddress + ":" + port + " | RECEIVED MARKER from: "+senderIp+":"+senderPort);

        Snapshot snap = new Snapshot(snapshotId, current_state); //Creates the snapshot and saves the current state!

        if (!runningSnapshots.contains(snap)) {
            System.out.println(ipAddress + ":" + port + " | First time receiving a marker");
            runningSnapshots.add(snap);
            recordSnapshotId(senderIp, senderPort, snapshotId);
            propagateMarker(initiatorIp, initiatorPort, snapshotId);
        }else{
            recordSnapshotId(senderIp, senderPort, snapshotId);
        }

        if (receivedMarkerFromAllLinks(snapshotId)) { //we have received a marker from all the channels

            Storage.writeFile(runningSnapshots,snapshotId);
            runningSnapshots.remove(snap);
        }

    }

    @Override
    public <MessageType> void receiveMessage(String senderIp, int senderPort, MessageType message) throws RemoteException {
        //for debug purposes
        //System.out.println(ipAddress + ":" + port + " | Received a message from remoteNode: " + senderIp + ":" + senderPort);

        //TODO: add the case to handle a remote node sending a message to me without him being in my remote nodes
        if (!runningSnapshots.isEmpty()) { //snapshot running, marker received
            //TODO: do not save the message when you have already received a marker from the same entity
            runningSnapshots.forEach( (snap) -> snap.messages.put(new Entity(senderIp,senderPort),message));
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
    public void removeMe(String ip_address, int port) throws RemoteException, SnapshotInterruptException {
        if(!this.runningSnapshots.isEmpty()) {
            System.out.println(ip_address+":"+port + " | ERROR: REMOVING DURING SNAPSHOT, ASSUMPTION NOT RESPECTED");
            throw new SnapshotInterruptException();
        }
        RemoteNode remoteNode = getRemoteNode(ip_address,port);
        this.remoteNodes.remove(remoteNode);
        appConnector.handleRemoveConnection(ip_address, port);
    }

    /**
     *
     * */
    private void recordSnapshotId(String senderIp, int senderPort, int snapshotId) throws DoubleMarkerException {
        RemoteNode remoteNode = getRemoteNode(senderIp,senderPort);
        if(remoteNode!=null) {
            if(remoteNode.snapshotIdsReceived.contains(snapshotId)){
                System.out.println(ipAddress +":"+port + " | ERROR: received multiple marker (same id) for the same link");
                throw new DoubleMarkerException();
            }else {
                System.out.println(ipAddress +":"+port + " | Added markerId for the remote node who called");
                remoteNode.snapshotIdsReceived.add(snapshotId);
            }
        }
    }

    /**
     * This methods sends a specific marker to all the connected RemoteNodes via RMI.
     * Together with the specific marker, also an identifier of the snapshot initiator
     * is propagated.
     * @param snapshotId the unique snapshot identifier (i.e. marker) that is being propagated
     * @param initiatorIp the IP address of the entity that initiated the snapshot
     * @param initiatorPort the port of the entity that initiated the snapshot
     * */
    private void propagateMarker(String initiatorIp, int initiatorPort, int snapshotId) {
        for (RemoteNode remoteNode : remoteNodes) {
            try {
                remoteNode.remoteInterface.receiveMarker(this.ipAddress, this.port, initiatorIp, initiatorPort, snapshotId);
            } catch (RemoteException| DoubleMarkerException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This methods retrieve the RemoteNode object associated to the ip/port couple by
     * performing a lookup in the list of stored RemoteNode objects, since each one
     * contains the ip/port as attributes. The association RemoteNode and ip/port is unique.
     * @param ip_address the IP address of the Remote Node to look up
     * @param port the port of the Remote Node to look up
     * */
    protected RemoteNode getRemoteNode(String ip_address, int port){
        for (RemoteNode remoteNode : remoteNodes) {
            if(remoteNode.ipAddress.equals(ip_address) && remoteNode.port==port)
                return remoteNode;
        }
        return null;
    }

    /**
     * This method checks if the same marker has been received by all nodes connected to the current node.
     * If all connected nodes have send a specific marker, it means that the related snapshot is over.
     * @param snapshotId the unique snapshot identifier (i.e. marker) to check.
     * */
    private boolean receivedMarkerFromAllLinks(int snapshotId){
        return remoteNodes.stream().filter(rn->rn.snapshotIdsReceived.contains(snapshotId)).count() == remoteNodes.size();
    }

    void setAppConnector(AppConnector o) {
        this.appConnector = o;
    }
}