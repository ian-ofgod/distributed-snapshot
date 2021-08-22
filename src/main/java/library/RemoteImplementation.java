package library;

import library.exceptions.DoubleMarkerException;
import library.exceptions.RestoreAlreadyInProgress;
import library.exceptions.SnapshotInterruptException;
import library.exceptions.UnexpectedMarkerReceived;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This is the implementation of the RemoteInterface. The RemoteInterface is the stub
 * that is available to others nodes to interact with this one.
 * It contains methods such as receiveMarker, receiveMessage, addMeBack and removeMe
 * @param <MessageType> this is the type that will be exchanged as a message between nodes
 * @param <StateType> this is the type that will be saved as the state of the application
 * */
    //TODO: testare un paio di metodi interni qui
    //TODO: ok that the functions do nothing in the not-ready state
class RemoteImplementation<StateType, MessageType>  implements RemoteInterface<MessageType> {

    /**
     * The hostname of the local node
     * */
    protected String hostname;

    /**
     * The RMI registry port of the local node
     * */
    protected int port;

    /**
     * Stores the current state: will fill the different Snapshot objects when created
     * */
    protected StateType currentState;

    /**
     * Lock object for currentState variable
     * */
    protected final Object currentStateLock = new Object();

    /**
     * It stores remote references to the linked nodes
     * */
    protected ArrayList<RemoteNode<MessageType>> remoteNodes = new ArrayList<>();

    /**
     * Provided implementation of the class AppConnector
     * */
    protected AppConnector<MessageType> appConnector;

    /**
     * List of the ids of running snapshots
     * */
    protected ArrayList<Snapshot<StateType, MessageType>> runningSnapshots = new ArrayList<>();

    /**
     * Counter that is increased each time this node starts a snapshot, it is used to compute the new snapshotId
     * */
    protected int localSnapshotCounter=0;

    /**
     * Variable that indicates if the node is able to perform modifying functions (true) or if the node is undergoing a
     * restore of a snapshot (false)
     */
    protected boolean nodeReady=true; //TODO: probably synchronize nodeReady

    /**
     * Handles the propagateMarker calls (see receiveMarker method)
     * */
    private final ExecutorService executors = Executors.newCachedThreadPool();

    private Snapshot<StateType, MessageType> currentSnapshotToBeRestored =  null;


    @Override
    public synchronized void receiveMarker(String senderHostname, int senderPort, String initiatorHostname, int initiatorPort, int snapshotId) throws DoubleMarkerException, UnexpectedMarkerReceived {
        //TODO: remove SystemPrintln
        System.out.println(hostname + ":" + port + " | RECEIVED MARKER from: " + senderHostname + ":" + senderPort);
        if (nodeReady) {
            if (checkIfRemoteNodePresent(senderHostname, senderPort)) { //TODO: is this check still needed?
                Snapshot<StateType, MessageType> snap;
                synchronized (currentStateLock) {
                    snap = new Snapshot<>(snapshotId, currentState, remoteNodes); //Creates the snapshot and saves the current state!
                }

                if (!runningSnapshots.contains(snap)) {
                    //TODO: remove SystemPrintln
                    System.out.println(hostname + ":" + port + " | First time receiving a marker");
                    runningSnapshots.add(snap);
                    recordSnapshotId(senderHostname, senderPort, snapshotId);
                    executors.submit(() -> propagateMarker(initiatorHostname, initiatorPort, snapshotId));
                } else {
                    recordSnapshotId(senderHostname, senderPort, snapshotId);
                }

                if (receivedMarkerFromAllLinks(snapshotId)) { //we have received a marker from all the channels
                    Storage.writeFile(runningSnapshots, snapshotId);
                    runningSnapshots.remove(snap);
                }
            } else {
                throw new UnexpectedMarkerReceived("ERROR: received a marker from a node not present in my remote nodes list");
            }
        } else {
            //TODO: what to do when the node is not ready?
        }
    }

    @Override
    public synchronized void receiveMessage(String senderHostname, int senderPort, MessageType message) throws RemoteException, NotBoundException, SnapshotInterruptException {
        if(nodeReady) {
            if (checkIfRemoteNodePresent(senderHostname, senderPort)) {
                if (!runningSnapshots.isEmpty()) { // Snapshot running
                    runningSnapshots.forEach((snap) -> {
                        if (!checkIfReceivedMarker(senderHostname, senderPort, snap.snapshotId)) {

                            snap.messages.add(new Envelope<>(new Entity(senderHostname, senderPort), message));
                        }
                    });
                }
                appConnector.handleIncomingMessage(senderHostname, senderPort, message);
            } else {
                // We issue the command to the remote node to remove us!
                Registry registry = LocateRegistry.getRegistry(senderHostname, senderPort);
                RemoteInterface<MessageType> remoteInterface = (RemoteInterface<MessageType>) registry.lookup("RemoteInterface");
                remoteInterface.removeMe(this.hostname, this.port);
            }
        }
    }

    @Override
    public synchronized void addMeBack(String hostname, int port) throws RemoteException, NotBoundException {
        if(nodeReady) {
            Registry registry = LocateRegistry.getRegistry(hostname, port);
            RemoteInterface<MessageType> remoteInterface = (RemoteInterface<MessageType>) registry.lookup("RemoteInterface");
            remoteNodes.add(new RemoteNode<>(hostname, port, remoteInterface));
            appConnector.handleNewConnection(hostname, port);
        }
    }


    @Override
    public synchronized void removeMe(String hostname, int port) throws RemoteException, SnapshotInterruptException {
        if(nodeReady) {
            if (!this.runningSnapshots.isEmpty()) {
                throw new SnapshotInterruptException(hostname + ":" + port + " | ERROR: REMOVING DURING SNAPSHOT, ASSUMPTION NOT RESPECTED");
            }
            RemoteNode<MessageType> remoteNode = getRemoteNode(hostname, port);
            this.remoteNodes.remove(remoteNode);
            appConnector.handleRemoveConnection(hostname, port);
        }
    }

    //:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //                      COMMODITY FUNCTIONS
    //:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    @Override
    public synchronized ArrayList<Entity> getNodes() {
        ArrayList<Entity> nodes = new ArrayList<>();
        for (RemoteNode<MessageType> node : remoteNodes) {
            nodes.add(new Entity(node.hostname, node.port));
        }
        return nodes;
    }

    /**
     * This function stores the provided snapshotId inside the provided remote entity
     * @param senderHostname the hostname of the entity in which the snapshotId will be recorded
     * @param senderPort the RMI registry port of the entity in which the snapshotId will be recorded
     * @param snapshotId the snapshot identifier to be recorded
     * @throws DoubleMarkerException received multiple marker (same id) from the same link
     */
    private void recordSnapshotId(String senderHostname, int senderPort, int snapshotId) throws DoubleMarkerException {
        RemoteNode<MessageType> remoteNode = getRemoteNode(senderHostname,senderPort);
        if(remoteNode!=null) {
            if(remoteNode.snapshotIdsReceived.contains(snapshotId)){
                throw new DoubleMarkerException(hostname +":"+port + " | ERROR: received multiple marker (same id) for the same link");
            }else {
                //TODO: remove SystemPrintln
                System.out.println(hostname +":"+port + " | Added markerId for the remote node who called");
                remoteNode.snapshotIdsReceived.add(snapshotId);
            }
        }
    }

    /**
     * This methods sends a specific marker to all the connected RemoteNodes via RMI.
     * Together with the specific marker, also an identifier of the snapshot initiator
     * is propagated
     * @param snapshotId the unique snapshot identifier (i.e. marker) that is being propagated
     * @param initiatorHostname the IP address of the entity that initiated the snapshot
     * @param initiatorPort the port of the entity that initiated the snapshot
     * */
    private synchronized void propagateMarker(String initiatorHostname, int initiatorPort, int snapshotId) {
        for (RemoteNode<MessageType> remoteNode : remoteNodes) {
            try {
                remoteNode.remoteInterface.receiveMarker(this.hostname, this.port, initiatorHostname, initiatorPort, snapshotId);
            } catch (RemoteException | DoubleMarkerException | UnexpectedMarkerReceived e) {
                System.err.println("Error in propagating marker!");
            }
        }
    }

    /**
     * This methods retrieve the RemoteNode object associated to the hostname/port couple by
     * performing a lookup in the list of stored RemoteNode objects, since each one
     * contains the hostname/port as attributes. The association RemoteNode and hostname/port is unique
     * @param hostname the hostname of the Remote Node to look up
     * @param port the port of the Remote Node to look up
     * */
    protected RemoteNode<MessageType> getRemoteNode(String hostname, int port) {
        for (RemoteNode<MessageType> remoteNode : remoteNodes) {
            if(remoteNode.hostname.equals(hostname) && remoteNode.port==port)
                return remoteNode;
        }
        return null;
    }

    /**
     * This method checks if the same marker has been received by all nodes connected to the current node.
     * If all connected nodes have send a specific marker, it means that the related snapshot is over
     * @param snapshotId the unique snapshot identifier (i.e. marker) to check
     * */
    private boolean receivedMarkerFromAllLinks(int snapshotId) {
        return remoteNodes.stream().filter(rn->rn.snapshotIdsReceived.contains(snapshotId)).count() == remoteNodes.size();
    }

    /**
     * This method checks if inside the remote node list it exists a node with the provided hostname and port
     * @param hostname the hostname of the node to search for
     * @param port the RMI registry port of the node to search for
     */
    private boolean checkIfRemoteNodePresent(String hostname, int port){
        for (RemoteNode<MessageType> remoteNode : remoteNodes){
            if(remoteNode.equals(new RemoteNode<>(hostname, port, null)))
                return true;
        }
        return false;
    }

    /**
     * This method checks if from the provided entity the local node has already received the marker
     * @param hostname the hostname of the provided entity
     * @param port the RMI port of the provided entity
     * @param snapshotId the identifier of the snapshot to check for
     */
    private boolean checkIfReceivedMarker(String hostname, int port, int snapshotId){
        for (RemoteNode<MessageType> remoteNode : remoteNodes){
            if(remoteNode.equals(new RemoteNode<>(hostname, port, null)))
                if(remoteNode.snapshotIdsReceived.contains(snapshotId))
                    return true;
        }
        return false;
    }

    //:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //                      RESTORE FUNCTIONS
    //:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    @Override
    public void restoreState(int snapshotId) throws RestoreAlreadyInProgress, RemoteException {
        if(currentSnapshotToBeRestored == null){
           currentSnapshotToBeRestored= Storage.readFile(snapshotId);
        }
        else if(snapshotId != currentSnapshotToBeRestored.snapshotId) {
            throw new RestoreAlreadyInProgress("CRITICAL ERROR: Another snapshot is being restored");
        }
        this.currentState=currentSnapshotToBeRestored.state; //TODO: @Luca should the modification on State be synchronized on StateLock?
    }

    @Override
    public void restoreConnections(int snapshotId) throws RestoreAlreadyInProgress, RemoteException, NotBoundException {
        if(currentSnapshotToBeRestored == null){
            currentSnapshotToBeRestored= Storage.readFile(snapshotId);
        }
        else if(snapshotId != currentSnapshotToBeRestored.snapshotId) {
            throw new RestoreAlreadyInProgress("CRITICAL ERROR: Another snapshot is being restored");
        }
        this.remoteNodes=new ArrayList<>();
        for (Entity entity : currentSnapshotToBeRestored.connectedNodes) {
            Registry registry = LocateRegistry.getRegistry(entity.getIpAddress(), entity.getPort());
            RemoteInterface<MessageType> remoteInterface = (RemoteInterface<MessageType>) registry.lookup("RemoteInterface");
            this.remoteNodes.add(new RemoteNode<>(entity.getIpAddress(), entity.getPort(), remoteInterface));
        }
    }

    @Override
    public void setReady(boolean value) throws RemoteException{
        // in the case of flipping the nodeReady bit from false to true we "reset" the currentSnapshotToBeRestored to null
        if(!nodeReady && value)
            currentSnapshotToBeRestored=null;
        this.nodeReady=value;
    }

    @Override
    public void restoreOldIncomingMessages(int snapshotId) throws RestoreAlreadyInProgress, RemoteException {
        if (nodeReady) {
            if(currentSnapshotToBeRestored == null){
                currentSnapshotToBeRestored= Storage.readFile(snapshotId);
            }
            else if(snapshotId != currentSnapshotToBeRestored.snapshotId) {
                throw new RestoreAlreadyInProgress("CRITICAL ERROR: Another snapshot is being restored");
            }
            for (Envelope<MessageType> envelope : currentSnapshotToBeRestored.messages) {
                this.appConnector.handleIncomingMessage(envelope.sender.getIpAddress(),envelope.sender.getPort(),envelope.message);
            }

        }
    }

}