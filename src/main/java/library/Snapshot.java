package library;

import java.util.*;

/**
 * This class describes a snapshot object that is available on a specific node.
 * It contains the current state at the moment of snapshot initialization, and the
 * list of messages that are received after the marker, as required by the
 * distributed snapshot algorithm
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
     * Map to store the list of received messages after the snapshot is started.
     * It keeps track of the sender of the message, in case the application needs it.
     * The sender of the message is stored in a specific class Entity.
     * */
    protected HashMap<Entity, ArrayList<MessageType>> messages = new HashMap<>();

    /**
     * Snapshot constructor with unique id. The snapshot is empty, as it
     * has not received the current state. Can be used to create temporary snapshot
     * objects to perform comparison via the Equals method, since snapshots are
     * identified via id.
     * @param id the snapshot unique identifier
     * */
    public Snapshot(int id){
        this.snapshotId = id;
    }

    /**
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

    @Override
    public String toString() {
        return "Snapshot "+snapshotId+"{" +
                ", state=" + state +
                ", messages= {}" + messages+
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Snapshot<?, ?> snapshot = (Snapshot<?, ?>) o;
        return snapshotId == snapshot.snapshotId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(snapshotId);
    }
}

/**
 * Class to uniquely identify a sender entity: it wraps the ipAddress and the port
 * where a message is coming from, making it easier to store the received messages
 * in an HashMap with the sender Entity as a unique key.
 * */
class Entity {
    /**
     * ipAddress string associated to this entity
     * */
    protected String ipAddress;

    /**
     * Port number associated to this entity
     * */
    protected int port;

    /**
     *
     * */
    public Entity(String ip_address, int port) {
        this.ipAddress = ip_address;
        this.port = port;
    }

    @Override
    public String toString() {
        return ipAddress + ':'+ port;
    }
}
