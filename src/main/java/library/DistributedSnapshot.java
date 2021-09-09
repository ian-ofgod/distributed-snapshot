package library;

import library.exceptions.*;

import java.io.*;
import java.rmi.*;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This is the main class of the distributed snapshot library. A DistributedSnapshot object must be created
 * in order to interact with the library. This implementation provides methods such as init, sendMessage,
 * updateState, addConnection, removeConnection and initiateSnapshot.
 * @param <StateType> this is the type that will be used to store the application state
 * @param <MessageType> this is the type that will be exchanged as a message between nodes
 * */
public class DistributedSnapshot<StateType, MessageType> {
    /**
     * The implementation of the remoteInterface used on this node
     * */
    protected final RemoteImplementation<StateType,MessageType> remoteImplementation = new RemoteImplementation<>();

    /**
     * Lock object
     * */
    protected ReadWriteLock distributedSnapshotLock = new ReentrantReadWriteLock();

    /**
     * This method is used to initialize a DistributedSnapshot object.
     * It sets the hostname, the port and the appConnector reference.
     * It starts the rmi registry and publishes the RemoteInterface in order to be reachable from other nodes.
     * @param yourHostname the hostname the application can be reached at
     * @param rmiRegistryPort the port used by the rmi registry
     * @param appConnector the reference to an appConnector implementation
     * @throws RemoteException communication-related exception that may occur during remote calls
     * @throws AlreadyBoundException the rmi registry has already bound a remote interface, try with another registry
     * @throws AlreadyInitialized this instance has been already initialized
     */
    public void init(String yourHostname, int rmiRegistryPort, AppConnector<MessageType, StateType> appConnector) throws RemoteException, AlreadyBoundException, AlreadyInitialized {
        distributedSnapshotLock.writeLock().lock();
        remoteImplementation.nodeStateLock.writeLock().lock();
        try {
            if (remoteImplementation.nodeState != NodeState.STARTED)
                throw new AlreadyInitialized("You are trying to initialize an instance that is already initialized");

            remoteImplementation.hostname = yourHostname;
            remoteImplementation.port = rmiRegistryPort;

            RemoteInterface<MessageType> stub = (RemoteInterface<MessageType>) UnicastRemoteObject.exportObject(remoteImplementation, 0);
            Registry registry = LocateRegistry.createRegistry(remoteImplementation.port);
            registry.bind("RemoteInterface", stub);

            remoteImplementation.appConnector = appConnector;
            remoteImplementation.nodeState = NodeState.DETACHED;
        } finally {
            remoteImplementation.nodeStateLock.writeLock().unlock();
            distributedSnapshotLock.writeLock().unlock();
        }
    }

    /**
     * This method is used to join the mesh network provided a gateway node to access it.
     * It populates the remoteImplementation.remoteNodes with the nodes of the network and their remoteInterfaces
     * @param hostname the hostname of one node in the network (will be our initial gateway)
     * @param port the port of our initial gateway to the network
     * @return an ArrayList of Entity (a class containing hostname and port) of all the nodes in the network
     * @throws RemoteException communication-related exception that may occur during remote calls
     * @throws NotBoundException thrown if an attempt is made to lookup or unbind in the registry a name that has no associated binding.
     * @throws NotInitialized thrown if an attempt to join the network is made before calling the init method of the library
     * @throws OperationForbidden thrown if an attempt to join the network is made through the node itself
     */
    public ArrayList<Entity> joinNetwork(String hostname, int port) throws RemoteException, NotBoundException, NotInitialized, OperationForbidden {
        distributedSnapshotLock.writeLock().lock();
        remoteImplementation.nodeStateLock.writeLock().lock();
        try {
            if (remoteImplementation.nodeState == NodeState.STARTED)
                throw new NotInitialized("You must initialize the library and join a network before trying to disconnect from it");
            if (remoteImplementation.nodeState != NodeState.DETACHED)
                throw new OperationForbidden("You are already connected to a network!");

            if (Objects.equals(hostname, this.remoteImplementation.hostname) && port == this.remoteImplementation.port)
                throw new OperationForbidden("You cannot join yourself");

            remoteImplementation.nodeSnapshotLock.writeLock().lock();
            ArrayList<Entity> networkNodes;
            try {
                Registry registry = LocateRegistry.getRegistry(hostname, port);
                RemoteInterface<MessageType> remoteInterface = (RemoteInterface<MessageType>) registry.lookup("RemoteInterface");
                networkNodes = remoteInterface.getConnections();
                this.remoteImplementation.remoteNodes = new ArrayList<>(); //reset current node connections
                this.remoteImplementation.remoteNodes.add(new RemoteNode<>(hostname, port, remoteInterface));
                remoteInterface.addMeBack(remoteImplementation.hostname, remoteImplementation.port);
                for (Entity entry : networkNodes) {
                    if (!Objects.equals(entry.getHostname(), this.remoteImplementation.hostname) || entry.getPort() != this.remoteImplementation.port) {
                        Registry nodeRegistry = LocateRegistry.getRegistry(entry.getHostname(), entry.getPort());
                        RemoteInterface<MessageType> nodeRemoteInterface = (RemoteInterface<MessageType>) nodeRegistry.lookup("RemoteInterface");
                        remoteImplementation.remoteNodes.add(new RemoteNode<>(entry.getHostname(), entry.getPort(), nodeRemoteInterface));
                        nodeRemoteInterface.addMeBack(remoteImplementation.hostname, remoteImplementation.port);
                    }
                }
            } finally {
                remoteImplementation.nodeSnapshotLock.writeLock().unlock();
            }
            networkNodes.add(new Entity(hostname, port));
            remoteImplementation.nodeState = NodeState.READY;
            return networkNodes;
        } finally {
            remoteImplementation.nodeStateLock.writeLock().unlock();
            distributedSnapshotLock.writeLock().unlock();
        }
    }

    /**
     * This method is used to send a message to a specific node by using rmi
     * @param hostname the hostname of the remote node
     * @param port the port associated to the rmi registry in the remote node
     * @param message the message to send to the remote node
     * @throws RemoteNodeNotFound the remote node is not found, you must first connect to it
     * @throws RemoteException communication-related exception that may occur during remote calls
     * @throws NotBoundException the remote node has not bound its remote implementation
     * @throws NotInitialized this instance hasn't been initialized, you must do it first
     * @throws SnapshotInterruptException it's not possible to remove a node when a snapshot is running
     * @throws RestoreInProgress thrown if a restore of a snapshot is in progress and the user tries to send a message
     * @throws OperationForbidden thrown if an attempt to send a message to the node itself is made
     */
    public void sendMessage(String hostname, int port, MessageType message) throws RemoteNodeNotFound, RemoteException, NotBoundException, NotInitialized, SnapshotInterruptException, RestoreInProgress, OperationForbidden {
        distributedSnapshotLock.readLock().lock();
        try {
            remoteImplementation.nodeStateLock.readLock().lock();
            try {
                if (remoteImplementation.nodeState == NodeState.STARTED || remoteImplementation.nodeState == NodeState.DETACHED) {
                    throw new NotInitialized("You must initialize the instance and connect to a network before sending any message");
                }
                if (remoteImplementation.nodeState == NodeState.RESTORING) {
                    throw new RestoreInProgress("A restore is in progress, please wait until node is ready");
                }
            } finally {
                remoteImplementation.nodeStateLock.readLock().unlock();
            }
            RemoteInterface<MessageType> remoteInterface;
            remoteImplementation.nodeSnapshotLock.writeLock().lock();
            try {
                // send the message only if we are not sending the message to this node
                if (!(hostname.equals(this.remoteImplementation.hostname) && port==this.remoteImplementation.port)) {
                    remoteInterface = getRemoteInterface(hostname, port);
                } else {
                    throw new OperationForbidden("You cannot send a message to yourself");
                }
            } finally {
                remoteImplementation.nodeSnapshotLock.writeLock().unlock();
            }
            try {
                remoteInterface.receiveMessage(remoteImplementation.hostname, remoteImplementation.port, message);
            } catch (RemoteException e) {
                Registry nodeRegistry = LocateRegistry.getRegistry(hostname, port);
                RemoteInterface<MessageType> nodeRemoteInterface = (RemoteInterface<MessageType>) nodeRegistry.lookup("RemoteInterface");
                remoteImplementation.nodeSnapshotLock.writeLock().lock();
                try {
                    this.remoteImplementation.getRemoteNode(hostname, port).remoteInterface = nodeRemoteInterface; //set the new remoteInterface
                } finally {
                    remoteImplementation.nodeSnapshotLock.writeLock().unlock();
                }
                nodeRemoteInterface.receiveMessage(remoteImplementation.hostname, remoteImplementation.port, message);
            }
        } finally {
            distributedSnapshotLock.readLock().unlock();
        }
    }

    /** This method is used to update the state. It makes a deep copy to store inside the remoteImplementation
     * @param state the object to save
     * @throws StateUpdateException something went wrong making a deep copy
     * @throws RestoreInProgress thrown if a restore of a snapshot is in progress and the user tries to update the state of the node
     * @throws NotInitialized thrown if an attempt to update the state is made before the node is correctly initialized
     * */
    public void updateState(StateType state) throws StateUpdateException, RestoreInProgress, NotInitialized {
        remoteImplementation.nodeStateLock.readLock().lock();
        try {
            if (remoteImplementation.nodeState == NodeState.STARTED) {
                throw new NotInitialized("You must initialize the instance before saving the state");
            }
            if (remoteImplementation.nodeState == NodeState.RESTORING) {
                throw new RestoreInProgress("A restore is in progress, please wait until node is ready");
            }
            synchronized (remoteImplementation.currentStateLock) {
                try {
                    this.remoteImplementation.currentState = deepClone(state); // assign currentState a deepCopy of the state provided by the user
                } catch (IOException | ClassNotFoundException e) {
                    StateUpdateException sue = new StateUpdateException("Problem in updating the state");
                    sue.setStackTrace(e.getStackTrace());
                    throw sue;
                }
            }
        } finally {
            remoteImplementation.nodeStateLock.readLock().unlock();
        }
    }

    /**
     * This method is used to start a snapshot with the distributed snapshot algorithm
     * @throws RemoteException communication-related exception that may occur during remote calls
     * @throws DoubleMarkerException received multiple marker (same id) from the same link
     * @throws UnexpectedMarkerReceived the sender node is not present in the remote nodes list
     * @throws NotInitialized this instance hasn't been initialized, you must do it first
     * @throws RestoreInProgress thrown when trying to start a snapshot while a restore is in progress in this node
     * */
    public void initiateSnapshot() throws IOException, DoubleMarkerException, UnexpectedMarkerReceived, NotInitialized, RestoreInProgress {
        distributedSnapshotLock.writeLock().lock();
        remoteImplementation.nodeStateLock.readLock().lock();
        try {
            if (remoteImplementation.nodeState == NodeState.STARTED || remoteImplementation.nodeState == NodeState.DETACHED)
                throw new NotInitialized("You must initialize the instance and connect to a network before starting a snapshot");
            if (remoteImplementation.nodeState == NodeState.RESTORING)
                throw new RestoreInProgress("A restore is in progress, please wait until node is ready");
            remoteImplementation.nodeSnapshotLock.writeLock().lock();
            int snapshotId;
            try {
                String snapshotIdString = remoteImplementation.hostname + remoteImplementation.port + remoteImplementation.localSnapshotCounter;
                snapshotId = snapshotIdString.hashCode();
                remoteImplementation.localSnapshotCounter++;
                Snapshot<StateType, MessageType> snap = new Snapshot<>(snapshotId, remoteImplementation.currentState, remoteImplementation.remoteNodes);
                remoteImplementation.runningSnapshots.add(snap);
            } finally {
                remoteImplementation.nodeSnapshotLock.writeLock().unlock();
            }
            // Assumption from the text: no change in the network topology is allowed during a snapshot!
            for (RemoteNode<MessageType> remoteNode : remoteImplementation.remoteNodes) {
                remoteNode.remoteInterface.receiveMarker(remoteImplementation.hostname, remoteImplementation.port, remoteImplementation.hostname, remoteImplementation.port, snapshotId);
            }
        } finally {
            remoteImplementation.nodeStateLock.readLock().unlock();
            distributedSnapshotLock.writeLock().unlock();
        }
    }

    /**
     * This is method is used to disconnect from the mesh network.
     * It does so by invoking removeMe on all connected nodes.
     * @throws SnapshotInterruptException it's not possible to remove a node when a snapshot is running
     * @throws NotInitialized this instance hasn't been initialized, you must do it first
     * @throws OperationForbidden it is not possible to remove a connection while a snapshot is running
     * @throws RestoreInProgress thrown if an attempt to disconnect this node from the network is made while a restore is in progress
     * */
    public void disconnect() throws OperationForbidden, SnapshotInterruptException, NotInitialized, RestoreInProgress {
        distributedSnapshotLock.writeLock().lock();
        remoteImplementation.nodeStateLock.writeLock().lock();
        try {
            if (remoteImplementation.nodeState == NodeState.STARTED || remoteImplementation.nodeState == NodeState.DETACHED)
                throw new NotInitialized("You must initialize the library and join a network before trying to disconnect from it");
            if (remoteImplementation.nodeState == NodeState.RESTORING)
                throw new RestoreInProgress("A restore is in progress, please wait until node is ready");
            remoteImplementation.nodeState = NodeState.DISCONNECTING;
        } finally {
            remoteImplementation.nodeStateLock.readLock().lock();
            remoteImplementation.nodeStateLock.writeLock().unlock();
            try {
                remoteImplementation.nodeSnapshotLock.writeLock().lock();
                try {
                    // Since no change in the network topology is allowed during a snapshot
                    // this function WON'T BE CALLED if any snapshot is running THIS IS AN ASSUMPTION FROM THE ASSIGNMENT
                    if (!remoteImplementation.runningSnapshots.isEmpty()) {
                        throw new OperationForbidden("Unable to disconnect from the network while snapshots are running");
                    }
                    for (RemoteNode<MessageType> remoteNode : remoteImplementation.remoteNodes) {
                        try {
                            remoteNode.remoteInterface.removeMe(remoteImplementation.hostname, remoteImplementation.port);
                        } catch (RemoteException ignored) {}
                    }
                    remoteImplementation.remoteNodes = new ArrayList<>();
                } finally {
                    remoteImplementation.nodeSnapshotLock.writeLock().unlock();
                }
            } finally {
                remoteImplementation.nodeStateLock.readLock().unlock();
            }
            remoteImplementation.nodeStateLock.writeLock().lock();
            remoteImplementation.nodeSnapshotLock.writeLock().lock();
            try {
                if (remoteImplementation.remoteNodes.size()==0)
                    remoteImplementation.nodeState = NodeState.DETACHED;
            } finally {
                remoteImplementation.nodeSnapshotLock.writeLock().unlock();
                remoteImplementation.nodeStateLock.writeLock().unlock();
            }
            distributedSnapshotLock.writeLock().unlock();
        }
    }

    /**
     * This method is used to un-export and unbind this remoteImplementation in the RMI registry
     * @throws RemoteException communication-related exception that may occur during remote calls
     * @throws NotBoundException thrown if an attempt is made to lookup or unbind in the registry a name that has no associated binding.
     */
    public void stop() throws NotBoundException, RemoteException {
        UnicastRemoteObject.unexportObject(remoteImplementation, true);
        LocateRegistry.getRegistry(remoteImplementation.port).unbind("RemoteInterface");
    }

    /**
     * This method is used to start restoring from the most recent snapshot available.
     * The node must be initialized before calling this method.
     * @throws RestoreAlreadyInProgress thrown when asking a remote node to restore while another restore is already in progress on the remote node
     * @throws RemoteException communication-related exception that may occur during remote calls
     * @throws NotBoundException thrown if an attempt is made to lookup or unbind in the registry a name that has no associated binding.
     * @throws RestoreInProgress thrown if we are trying to restore while a restore is already in progress in our node
     * @throws RestoreNotPossible thrown if the restore was not possible, reason specified in the exception message (for example a node is no more reachable)
     * @throws ClassNotFoundException thrown when the storage facility is not able to reconstruct the Snapshot from the file
     */
    public void restoreLastSnapshot() throws RestoreAlreadyInProgress, IOException, NotBoundException, RestoreInProgress, RestoreNotPossible, ClassNotFoundException, OperationForbidden {
        System.out.println("["+ remoteImplementation.hostname+":"+ remoteImplementation.port+"] INITIATING RESTORE LAST SNAPSHOT #######################");
        distributedSnapshotLock.writeLock().lock();
        remoteImplementation.nodeStateLock.writeLock().lock();
        remoteImplementation.nodeSnapshotLock.writeLock().lock();
        try {
            if (remoteImplementation.nodeState == NodeState.RESTORING)
                throw new RestoreInProgress("A restore is in progress, please wait until node is ready");

            if (remoteImplementation.nodeState != NodeState.DETACHED)
                throw new OperationForbidden("You cannot restore a snapshot while connected to a network");

            int snapshotToRestore = Storage.getLastSnapshotId(remoteImplementation.hostname, remoteImplementation.port);

            // we set our node to the not-ready state and restore our connections and state according to our snapshot
            remoteImplementation.nodeState = NodeState.RESTORING;
            try {
                this.remoteImplementation.restoreConnections(snapshotToRestore);
            } catch (RestoreNotPossible e){
                //if a restore is not possible we should restore the state of the node to ready=true
                // by re-throwing the exception we let the user handle this case
                this.remoteImplementation.nodeState = NodeState.READY;
                throw e;
            }
            this.remoteImplementation.restoreState(snapshotToRestore);

            // we set all the nodes in our new connections list to the not-ready state and proceed to set their connection
            // list and state according to their snapshot
            // those calls should not be parallelized: if the
            System.out.println("["+ remoteImplementation.hostname+":"+ remoteImplementation.port+"] STARTING RESTORE ON OTHER NODES #######################");

            for (RemoteNode<MessageType> remoteNode : this.remoteImplementation.remoteNodes) {
                System.out.println("STARTING RESTORE ON ["+remoteNode.port+"]");
                remoteNode.remoteInterface.setReady(false);
                remoteNode.remoteInterface.restoreConnections(snapshotToRestore);
                remoteNode.remoteInterface.restoreState(snapshotToRestore);
                System.out.println("ENDING RESTORE ON ["+remoteNode.port+"]");
            }

            System.out.println("["+ remoteImplementation.hostname+":"+ remoteImplementation.port+"] RESTORE ENDED #######################");


            // now all the nodes can be set to the ready state
            // TODO: what if the application is automated (like sending a message every X seconds)?
            //       in this case the application would start as soon as the ready state is set to true,
            //       without restoring the incoming messages
            System.out.println("["+ remoteImplementation.hostname+":"+ remoteImplementation.port+"] SETTING nodeReady=true #######################");
            remoteImplementation.nodeState = NodeState.READY;
            for (RemoteNode<MessageType> remoteNode : this.remoteImplementation.remoteNodes) {
                remoteNode.remoteInterface.setReady(true);
            }
            //now all the "modifying" functions can be called again, hence we will start handling the old messages

            //handle old incoming messages
            System.out.println("["+ remoteImplementation.hostname+":"+ remoteImplementation.port+"] HANDLE OLD MESSAGES #######################");
            this.remoteImplementation.restoreOldIncomingMessages(snapshotToRestore);
            for (RemoteNode<MessageType> remoteNode : this.remoteImplementation.remoteNodes) {
                remoteNode.remoteInterface.restoreOldIncomingMessages(snapshotToRestore);
            }
        } finally {
            remoteImplementation.nodeSnapshotLock.writeLock().unlock();
            remoteImplementation.nodeStateLock.writeLock().unlock();
            distributedSnapshotLock.writeLock().unlock();
        }
        System.out.println("["+ remoteImplementation.hostname+":"+ remoteImplementation.port+"] FINISHED RESTORE LAST SNAPSHOT #######################");

    }

    /**
     * Remove the specified node from the network by telling everyone to do so
     * @param hostname the hostname of the node to remove
     * @param port the port of the node to remove
     * @throws RemoteException communication-related exception that may occur during remote calls
     * @throws RestoreInProgress thrown when trying to remove a node while a snapshot restore is in progress
     * @throws NotInitialized thrown if an attempt to ask to remove a node is made before this node is correctly initialized
     * @throws SnapshotInterruptException thrown if an attempt to remove a node (change in the network topology) is made while a snapshot is running
     */
    public void removeNode(String hostname, int port) throws RemoteException, RestoreInProgress, NotInitialized, SnapshotInterruptException {
        distributedSnapshotLock.writeLock().lock();
        remoteImplementation.nodeStateLock.readLock().lock();
        try {
            if (remoteImplementation.nodeState == NodeState.RESTORING)
                throw new RestoreInProgress("A restore is in progress, please wait until node is ready");
            if (remoteImplementation.nodeState == NodeState.STARTED || remoteImplementation.nodeState == NodeState.DETACHED)
                throw new NotInitialized("You must initialize the library and join a network before trying to disconnect a node from it");
            remoteImplementation.nodeSnapshotLock.writeLock().lock();
            try {
                this.remoteImplementation.remoteNodes.remove(this.remoteImplementation.getRemoteNode(hostname, port));
                for (RemoteNode<MessageType> remoteNode : this.remoteImplementation.remoteNodes) {
                    remoteNode.remoteInterface.removeMe(hostname, port);
                }
            } finally {
                remoteImplementation.nodeSnapshotLock.writeLock().unlock();
            }
        } finally {
            remoteImplementation.nodeStateLock.readLock().unlock();
            distributedSnapshotLock.writeLock().unlock();
        }
    }


    //##############################################################
    //              COMMODITY FUNCTIONS
    //##############################################################

    /**
     * This is method is used to get the reference of a RemoteNode
     * @param hostname the hostname of the remote node
     * @param port the RMI registry port of the remote node
     * @throws RemoteNodeNotFound the remote node is not found
     * */
    private RemoteInterface<MessageType> getRemoteInterface(String hostname, int port) throws RemoteNodeNotFound {
        int index= remoteImplementation.remoteNodes.indexOf(new RemoteNode<MessageType>(hostname,port,null));
        if(index==-1){
           throw new RemoteNodeNotFound("RemoteNode with the following hostname " + hostname +
                   " and port " +port+
                   " not found");
        }
        RemoteNode<MessageType> remoteNode = remoteImplementation.remoteNodes.get(index);
        return remoteNode.remoteInterface;
    }

    /**
     * Courtesy of
     * www.infoworld.com/article/2077578/java-tip-76--an-alternative-to-the-deep-copy-technique.html
     *
     * Given that we decided to make a deep copy of a serializable object this trick allows us
     * to make it by using only properties deriving from the fact that that object is serializable.
     * So no Cloneable or similar approaches, that would have implied that the user must create a State class
     * with specific characteristics mandated by the library (so not completely State agnostic)
     *
     * @param state The state provided by the user
     */
    private StateType deepClone(StateType state) throws IOException, ClassNotFoundException {
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            // First serializing the object and its state to memory using
            // ByteArrayOutputStream instead of FileOutputStream.
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(bos);
            out.writeObject(state);
            out.flush();

            // And then deserializing it from memory using ByteArrayOutputStream instead of FileInputStream.
            // Deserialization process will create a new object with the same state as in the serialized object,
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            in = new ObjectInputStream(bis);
            return (StateType) in.readObject();
        } finally {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        }
    }
}

/** Data class that encapsulates the structure of a Remote Node.
 * It contains the node identifiers (hostname and port) as well as the
 * reference to its corresponding Remote RMI Interface.
 * The class is also
 * used to keep track of the Snapshot marker received by the corresponding
 * Remote Node.
 * @param <MessageType> this is the type that will be exchanged as a message between nodes
 * */
class RemoteNode<MessageType> {

    /**
     * Hostname string associated to this entity
     * */
    protected String hostname;

    /**
     * Port number associated to this entity
     * */
    protected int port;

    /**
     * A reference to the Remote RMI Interface associated to this Remote Node
     * */
    protected RemoteInterface<MessageType> remoteInterface;

    /**
     * A List of Integers containing the IDs of the snapshots for which
     * the corresponding marker has been received by this Remote Node.
     * It is used to enable management of multiple, parallel, snapshots.
     * */
    protected ArrayList<Integer> snapshotIdsReceived = new ArrayList<>(); //holds the marker.id received from this remoteNode (for multiple concurrent distributed snapshots)

    /**
     * Constructor for the Remote Node object, it allows encapsulation of hostname
     * and port, and provides a reference to the Remote RMI Interface corresponding
     * to the created Remote Node
     * */
    public RemoteNode(String hostname, int port, RemoteInterface<MessageType> remoteInterface) {
        this.hostname = hostname;
        this.port = port;
        this.remoteInterface = remoteInterface;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteNode<MessageType> that = (RemoteNode<MessageType>) o;
        return port == that.port && Objects.equals(hostname, that.hostname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, port);
    }

}