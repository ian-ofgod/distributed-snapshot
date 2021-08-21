package library;

import java.util.*;

/**
 * This class describes a snapshot object that is available on a specific node.
 * It contains the current state at the moment of snapshot initialization, and the
 * list of messages that are received after the marker, as required by the
 * distributed snapshot algorithm
 * @param <MessageType> this is the type that will be exchanged as a message between nodes
 * @param <StateType> this is the type that will be saved as the state of the application
 * */
class Snapshot<StateType, MessageType> {

    /**
     * A unique identifier of the current snapshot
     * */
    protected int snapshotId;

    /**
     * The state of the current Node where the snapshot is running at the moment of
     * snapshot initialization or the reception of a first marker from the initiator
     * */
    protected StateType state;


    /**
     * The list of nodes connected at the beginning of the snapshot
     * it allows to recover the exact topology we had at the moment of the
     * snapshot.
     * */
    protected ArrayList<Entity> connectedNodes = new ArrayList<>();

    /**
     * Map to store the list of received messages after the snapshot is started.
     * It keeps track of the sender of the message, in case the application needs it.
     * The sender of the message is stored in a specific class Entity.
     * */
    protected ArrayList<Envelope> messages = new ArrayList<>();

    /**
     * Snapshot constructor that builds a full snapshot objects. Additionally to
     * the unique snapshot identifier, this constructor also stores the state of
     * the current node. The snapshot created with this constructor will be stored
     * in the current node.
     * @param id the snapshot unique identifier
     * @param state  the current state of the node at the moment of the snapshot initialization
     * @param remoteNodes the list of connected nodes at the moment of snapshot initialization
     * */
    public Snapshot(int id, StateType state, ArrayList<RemoteNode<MessageType>> remoteNodes){
        this.snapshotId = id;
        this.state = state;

        for (RemoteNode node : remoteNodes)
        {
            Entity connectedNode = new Entity(node.hostname, node.port);
            connectedNodes.add(connectedNode);
        }


    }

    /** CONSTRUCTOR needed for tests without storing connectedNodes.
     * todo: check if we can remove it
     * Snapshot constructor that builds a full snapshot objects. Additionally to
     * the unique snapshot identifier, this constructor also stores the state of
     * the current node. The snapshot created with this constructor will be stored
     * in the current node.
     * @param id the snapshot unique identifier
     * @param state  the current state of the node at the moment of the snapshot initialization
     * */
    public Snapshot(int id, StateType state){
        this.snapshotId = id;
        this.state = state;
    }


    /**
     * Snapshot constructor that builds an empty snapshot objects.
     * It only stores the unique snapshot identifier.
     * It is used for loading Snapshots from disk.
     * @param id the snapshot unique identifier
     * */
    public Snapshot(int id){
        this.snapshotId = id;
    }

    @Override
    public String toString() {
        return "Snapshot{" +
                "snapshotId=" + snapshotId +
                ", state=" + state +
                ", messages=" + messages +
                ", connectedNodes=" + connectedNodes +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Snapshot<?, ?> snapshot = (Snapshot<?, ?>) o;
        return this.snapshotId == snapshot.snapshotId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(snapshotId);
    }


}

class Envelope<MessageType>{
    protected Entity sender;
    protected MessageType message;

    public Envelope(Entity sender, MessageType message) {
        this.sender = sender;
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Envelope<MessageType> envelope = (Envelope<MessageType>) o;
        return sender.equals(envelope.sender) && message.equals(envelope.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sender, message);
    }

    @Override
    public String toString() {
        return "Envelope{" +
                "sender=" + sender +
                ", message=" + message +
                '}';
    }
}


