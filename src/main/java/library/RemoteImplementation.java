package library;

import library.exceptions.DoubleMarkerException;
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
 *
 * */
class RemoteImplementation<StateType, MessageType>  implements RemoteInterface<MessageType> {

    /**
     *
     * */
    protected String hostname;

    /**
     *
     * */
    protected int port;


    /**
     * Stores the current state: will fill the different Snapshot objects when created
     * */
    protected StateType current_state;

    /**
     * Lock object for currentState variable
     * */
    protected final Object currentStateLock = new Object();

    //store remote references to the linked nodes
    /**
     *
     * */
    protected ArrayList<RemoteNode<MessageType>> remoteNodes = new ArrayList<>();

    //this is the provided implementation of the class Observer
    /**
     *
     * */
    protected AppConnector<MessageType> appConnector;

    //list of the ids of running snapshots
    /**
     *
     * */
    protected ArrayList<Snapshot<StateType, MessageType>> runningSnapshots = new ArrayList<>();

    //counter that is increased each time this node starts a snapshot, it is used to compute the new snapshotId
    protected int localSnapshotCounter=0;

    /**
     * Handles the propagateMarker calls (see receiveMarker method)
     * */
    private final ExecutorService executors = Executors.newCachedThreadPool();

    @Override
    public synchronized void receiveMarker(String senderHostname, int senderPort, String initiatorHostname, int initiatorPort, int snapshotId) throws RemoteException, DoubleMarkerException, UnexpectedMarkerReceived {
        System.out.println(hostname + ":" + port + " | RECEIVED MARKER from: "+senderHostname+":"+senderPort);

        if(checkIfRemoteNodePresent(senderHostname,senderPort)) {
            Snapshot<StateType, MessageType> snap;
            synchronized (currentStateLock) {
                snap = new Snapshot<>(snapshotId, current_state); //Creates the snapshot and saves the current state!
            }

            if (!runningSnapshots.contains(snap)) {
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
        }else{
            throw new UnexpectedMarkerReceived("ERROR: received a marker from a node not present in my remote nodes list");
        }
    }

    @Override
    public synchronized void receiveMessage(String senderHostname, int senderPort, MessageType message) throws RemoteException, NotBoundException, SnapshotInterruptException {
        if(checkIfRemoteNodePresent(senderHostname, senderPort)) {
            if (!runningSnapshots.isEmpty()) { //snapshot running, marker received
                runningSnapshots.forEach((snap) -> {
                    if(!checkIfReceivedMarker(senderHostname,senderPort,snap.snapshotId)) {
                        snap.messages.computeIfAbsent(new Entity(senderHostname, senderPort), k -> new ArrayList<>());
                        snap.messages.get(new Entity(senderHostname,senderPort)).add(message);
                    }
                });
            }
            appConnector.handleIncomingMessage(senderHostname, senderPort, message);
        }else{
            //we issue the command to the remote node to remove us!
            Registry registry = LocateRegistry.getRegistry(senderHostname, senderPort);
            RemoteInterface<MessageType> remoteInterface = (RemoteInterface<MessageType>) registry.lookup("RemoteInterface");
            remoteInterface.removeMe(this.hostname, this.port);
        }
    }


    @Override
    public synchronized void addMeBack(String hostname, int port) throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(hostname, port);
        RemoteInterface<MessageType> remoteInterface = (RemoteInterface<MessageType>) registry.lookup("RemoteInterface");
        remoteNodes.add(new RemoteNode<>(hostname, port, remoteInterface));
        appConnector.handleNewConnection(hostname,port);
    }


    @Override
    public synchronized void removeMe(String hostname, int port) throws RemoteException, SnapshotInterruptException {
        if(!this.runningSnapshots.isEmpty()) {
            System.out.println(hostname+":"+port + " | ERROR: REMOVING DURING SNAPSHOT, ASSUMPTION NOT RESPECTED");
            throw new SnapshotInterruptException();
        }
        RemoteNode<MessageType> remoteNode = getRemoteNode(hostname,port);
        this.remoteNodes.remove(remoteNode);
        appConnector.handleRemoveConnection(hostname, port);
    }

    /**
     *
     * */
    private void recordSnapshotId(String senderHostname, int senderPort, int snapshotId) throws DoubleMarkerException {
        RemoteNode<MessageType> remoteNode = getRemoteNode(senderHostname,senderPort);
        if(remoteNode!=null) {
            if(remoteNode.snapshotIdsReceived.contains(snapshotId)){
                System.out.println(hostname +":"+port + " | ERROR: received multiple marker (same id) for the same link");
                throw new DoubleMarkerException();
            }else {
                System.out.println(hostname +":"+port + " | Added markerId for the remote node who called");
                remoteNode.snapshotIdsReceived.add(snapshotId);
            }
        }
    }

    //TODO: printStackTrace()
    /**
     * This methods sends a specific marker to all the connected RemoteNodes via RMI.
     * Together with the specific marker, also an identifier of the snapshot initiator
     * is propagated.
     * @param snapshotId the unique snapshot identifier (i.e. marker) that is being propagated
     * @param initiatorHostname the IP address of the entity that initiated the snapshot
     * @param initiatorPort the port of the entity that initiated the snapshot
     * */
    private synchronized void propagateMarker(String initiatorHostname, int initiatorPort, int snapshotId) {
        for (RemoteNode<MessageType> remoteNode : remoteNodes) {
            try {
                remoteNode.remoteInterface.receiveMarker(this.hostname, this.port, initiatorHostname, initiatorPort, snapshotId);
            } catch (RemoteException | DoubleMarkerException | UnexpectedMarkerReceived e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This methods retrieve the RemoteNode object associated to the ip/port couple by
     * performing a lookup in the list of stored RemoteNode objects, since each one
     * contains the ip/port as attributes. The association RemoteNode and ip/port is unique.
     * @param hostname the IP address of the Remote Node to look up
     * @param port the port of the Remote Node to look up
     * */
    protected RemoteNode<MessageType> getRemoteNode(String hostname, int port){
        for (RemoteNode<MessageType> remoteNode : remoteNodes) {
            if(remoteNode.hostname.equals(hostname) && remoteNode.port==port)
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

    private boolean checkIfRemoteNodePresent(String hostname, int port){
        for (RemoteNode<MessageType> remoteNode : remoteNodes){
            if(remoteNode.equals(new RemoteNode<>(hostname, port, null)))
                return true;
        }
        return false;
    }

    private boolean checkIfReceivedMarker(String hostname, int port, int snapshotId){
        for (RemoteNode<MessageType> remoteNode : remoteNodes){
            if(remoteNode.equals(new RemoteNode<>(hostname, port, null)))
                if(remoteNode.snapshotIdsReceived.contains(snapshotId))
                    return true;
        }
        return false;
    }
}