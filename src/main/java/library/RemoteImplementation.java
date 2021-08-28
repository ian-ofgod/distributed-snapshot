package library;

import library.exceptions.*;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This is the implementation of the RemoteInterface. The RemoteInterface is the stub
 * that is available to others nodes to interact with this one.
 * It contains methods such as receiveMarker, receiveMessage, addMeBack and removeMe
 * @param <MessageType> this is the type that will be exchanged as a message between nodes
 * @param <StateType> this is the type that will be saved as the state of the application
 * */
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
    protected AppConnector<MessageType, StateType> appConnector;

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
    protected boolean nodeReady = true;

    /**
     * Lock object for nodeReady variable
     * */
    protected final ReadWriteLock nodeReadyLock = new ReentrantReadWriteLock();

    /**
     * Handles the propagateMarker calls (see receiveMarker method) and handleIncomingMessage in receiveMessage
     * */
    private final ExecutorService executors = Executors.newCachedThreadPool();

    /**
     * Stores the current snapshot that is being restored
     */
    private Snapshot<StateType, MessageType> currentSnapshotToBeRestored =  null;


    @Override
    public synchronized void receiveMarker(String senderHostname, int senderPort, String initiatorHostname, int initiatorPort, int snapshotId) throws DoubleMarkerException, UnexpectedMarkerReceived, IOException {
        this.nodeReadyLock.readLock().lock();
        try {
            if (nodeReady) {
                if (checkIfRemoteNodePresent(senderHostname, senderPort)) {
                    Snapshot<StateType, MessageType> snap;
                    synchronized (currentStateLock) {
                        snap = new Snapshot<>(snapshotId, currentState, remoteNodes); //Creates the snapshot and saves the current state!
                    }

                    if (!runningSnapshots.contains(snap)) {
                        //This is the first time we receive a marker,
                        // so we HAVE TO propagate the marker to the other nodes
                        runningSnapshots.add(snap);
                        recordSnapshotId(senderHostname, senderPort, snapshotId);
                        executors.submit(() -> propagateMarker(initiatorHostname, initiatorPort, snapshotId));
                    } else {
                        // we have already received a marker for this snapshotId,
                        // so we don't have to propagate the marker to other nodes
                        recordSnapshotId(senderHostname, senderPort, snapshotId);
                    }

                    if (receivedMarkerFromAllLinks(snapshotId)) { //we have received a marker from all the channels
                        Storage.writeFile(runningSnapshots, snapshotId, this.hostname, this.port);
                        runningSnapshots.remove(snap);
                    }
                } else {
                    System.out.println("Remote node not present!");
                    throw new UnexpectedMarkerReceived("ERROR: received a marker from a node not present in my remote nodes list");
                }
            }
        } finally {
            this.nodeReadyLock.readLock().unlock();
        }
    }

    @Override
    public synchronized void receiveMessage(String senderHostname, int senderPort, MessageType message) throws RemoteException, NotBoundException, SnapshotInterruptException {
        System.out.println("Starting handling message");
        this.nodeReadyLock.readLock().lock();
        System.out.println("LOCK PRESO");
        try {
            if (nodeReady) {
                if (checkIfRemoteNodePresent(senderHostname, senderPort)) {
                    if (!runningSnapshots.isEmpty()) { // Snapshot running
                        runningSnapshots.forEach((snap) -> {
                            if (!checkIfReceivedMarker(senderHostname, senderPort, snap.snapshotId)) {
                                snap.messages.add(new Envelope<>(new Entity(senderHostname, senderPort), message));
                            }
                        });
                    }
                    System.out.println("executors: receiveMessage->handleIncomingMessage");
                    executors.submit(() -> appConnector.handleIncomingMessage(senderHostname, senderPort, message));
                } else {
                    // We issue the command to the remote node to remove us!
                    //TODO: given the mesh topology should we keep this case? Or we should do the opposite (add the node)?
                    Registry registry = LocateRegistry.getRegistry(senderHostname, senderPort);
                    RemoteInterface<MessageType> remoteInterface = (RemoteInterface<MessageType>) registry.lookup("RemoteInterface");
                    remoteInterface.removeMe(this.hostname, this.port);
                }
            }
        } finally {
            this.nodeReadyLock.readLock().unlock();
        }
    }

    @Override
    public synchronized void addMeBack(String hostname, int port) throws RemoteException, NotBoundException {
        this.nodeReadyLock.readLock().lock();
        try {
            if (nodeReady) {
                Registry registry = LocateRegistry.getRegistry(hostname, port);
                RemoteInterface<MessageType> remoteInterface = (RemoteInterface<MessageType>) registry.lookup("RemoteInterface");
                if (getRemoteNode(hostname, port) == null) {
                    remoteNodes.add(new RemoteNode<>(hostname, port, remoteInterface));
                    appConnector.handleNewConnection(hostname, port);
                }
            }
        } finally {
            this.nodeReadyLock.readLock().unlock();
        }
    }


    @Override
    public synchronized void removeMe(String hostname, int port) throws RemoteException, SnapshotInterruptException {
        this.nodeReadyLock.readLock().lock();
        try {
            if (nodeReady) {
                if (!this.runningSnapshots.isEmpty()) {
                    throw new SnapshotInterruptException(hostname + ":" + port + " | ERROR: REMOVING DURING SNAPSHOT, ASSUMPTION NOT RESPECTED");
                }
                RemoteNode<MessageType> remoteNode = getRemoteNode(hostname, port);
                this.remoteNodes.remove(remoteNode);
                appConnector.handleRemoveConnection(hostname, port);
            }
        } finally {
            this.nodeReadyLock.readLock().unlock();
        }
    }



    //:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //                      RESTORE FUNCTIONS
    //:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    @Override
    public void restoreState(int snapshotId) throws RestoreAlreadyInProgress, IOException, ClassNotFoundException {
        this.nodeReadyLock.readLock().lock();
        try {
            if (!nodeReady) {
                if (currentSnapshotToBeRestored == null) {
                    currentSnapshotToBeRestored = Storage.readFile(snapshotId, this.hostname, this.port);
                } else if (snapshotId != currentSnapshotToBeRestored.snapshotId) {
                    throw new RestoreAlreadyInProgress("CRITICAL ERROR: Another snapshot is being restored");
                }
                synchronized (currentStateLock) {
                    this.currentState = currentSnapshotToBeRestored.state;
                }
                appConnector.handleRestoredState(this.currentState);
            }
        } finally {
            this.nodeReadyLock.readLock().unlock();
        }
    }

    @Override
    public void restoreConnections(int snapshotId) throws RestoreAlreadyInProgress, IOException, RestoreNotPossible, ClassNotFoundException {
        this.nodeReadyLock.readLock().lock();
        try {
            if (!nodeReady) {
                if (currentSnapshotToBeRestored == null) {
                    currentSnapshotToBeRestored = Storage.readFile(snapshotId, this.hostname, this.port);
                } else if (snapshotId != currentSnapshotToBeRestored.snapshotId) {
                    throw new RestoreAlreadyInProgress("CRITICAL ERROR: Another snapshot is being restored");
                }
                this.remoteNodes = new ArrayList<>();
                ArrayList<RemoteNode<MessageType>> tempList= new ArrayList<>();
                for (Entity entity : currentSnapshotToBeRestored.connectedNodes) {
                    try {
                        Registry registry = LocateRegistry.getRegistry(entity.getHostname(), entity.getPort());
                        RemoteInterface<MessageType> remoteInterface = (RemoteInterface<MessageType>) registry.lookup("RemoteInterface");
                        tempList.add(new RemoteNode<>(entity.getHostname(), entity.getPort(), remoteInterface));
                    }catch(RemoteException | NotBoundException e){
                        throw new RestoreNotPossible("["+entity.getHostname()+":"+entity.getPort()+"] NOT AVAILABLE");
                    }
                }
                this.remoteNodes=tempList;
                appConnector.handleRestoredConnections(currentSnapshotToBeRestored.connectedNodes);
            }
        } finally {
            this.nodeReadyLock.readLock().unlock();
        }
    }

    @Override
    public void setReady(boolean value) throws RemoteException{
        this.nodeReadyLock.writeLock().lock();
        try {
            // in the case of flipping the nodeReady bit from false to true we "reset" the currentSnapshotToBeRestored to null
            if(!nodeReady && value)
                currentSnapshotToBeRestored=null;
            this.nodeReady=value;
        } finally {
            this.nodeReadyLock.writeLock().unlock();
        }
    }

    @Override
    public void forgetThisNode(String hostname, int port) throws RemoteException {
        this.nodeReadyLock.readLock().lock();
        try {
            if (nodeReady) {
                if (getRemoteNode(hostname, port) != null) {
                    this.remoteNodes.remove(getRemoteNode(hostname, port));
                }
            }
        } finally {
            this.nodeReadyLock.readLock().unlock();
        }
    }

    @Override
    public void restoreOldIncomingMessages(int snapshotId) throws RestoreAlreadyInProgress, IOException, ClassNotFoundException {
        this.nodeReadyLock.readLock().lock();
        try {
            if (nodeReady) {
                if (currentSnapshotToBeRestored == null) {
                    currentSnapshotToBeRestored = Storage.readFile(snapshotId, this.hostname, this.port);
                } else if (snapshotId != currentSnapshotToBeRestored.snapshotId) {
                    throw new RestoreAlreadyInProgress("CRITICAL ERROR: Another snapshot is being restored");
                }
                executors.submit(()->{
                    for (Envelope<MessageType> envelope : currentSnapshotToBeRestored.messages) {
                        this.appConnector.handleIncomingMessage(envelope.sender.getHostname(), envelope.sender.getPort(), envelope.message);
                    }
                });
            }
        } finally {
            this.nodeReadyLock.readLock().unlock();
        }
    }

    @Override
    public synchronized ArrayList<Entity> getConnections() {
        ArrayList<Entity> nodes = new ArrayList<>();
        for (RemoteNode<MessageType> node : remoteNodes) {
            nodes.add(new Entity(node.hostname, node.port));
        }
        return nodes;
    }



    //:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //                      COMMODITY FUNCTIONS
    //:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


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
                remoteNode.snapshotIdsReceived.add(snapshotId);
            }
        }else{
            System.out.println("RemoteNode not found!");
        }
    }

    /**
     * This method sends a specific marker to all the connected RemoteNodes via RMI.
     * Together with the specific marker, also an identifier of the snapshot initiator
     * is propagated
     * @param snapshotId the unique snapshot identifier (i.e. marker) that is being propagated
     * @param initiatorHostname the IP address of the entity that initiated the snapshot
     * @param initiatorPort the port of the entity that initiated the snapshot
     * */
    private void propagateMarker(String initiatorHostname, int initiatorPort, int snapshotId) {
        for (RemoteNode<MessageType> remoteNode : this.remoteNodes) {
            try {
                System.out.println("["+this.hostname + ":" + this.port + "] Sending MARKER to: " + remoteNode.hostname + ":" + remoteNode.port);
                remoteNode.remoteInterface.receiveMarker(this.hostname, this.port, initiatorHostname, initiatorPort, snapshotId);
            }
            catch (Exception e){
                //TODO: this function is used as a lambda so the exception is not rethrown... should we asses this?
                System.err.println("Error in propagating marker!");
            }
        }
        System.out.println("FINE propagazione PER NODO ["+this.hostname+":"+this.port+"]");
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
}