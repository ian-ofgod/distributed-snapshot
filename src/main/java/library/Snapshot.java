package library;

import java.util.*;

/**
 * Contains the snapshot of a single node
 * */
class Snapshot<StateType, MessageType> {

    protected int snapshotId;
    protected StateType state;
    protected HashMap<Entity, ArrayList<MessageType>> messages = new HashMap<>();


    public Snapshot(){ //TODO: lasciare solo il costruttore che serve
    }

    public Snapshot(int id){
        this.snapshotId = id;
    }

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


class Entity {
    /**
     * ipAddress string associated to this entity
     * */
    protected String ipAddress;

    /**
     * Port number associated to this entity
     * */
    protected int port;

    public Entity(String ip_address, int port) {
        this.ipAddress = ip_address;
        this.port = port;
    }

    @Override
    public String toString() {
        return ipAddress + ':'+ port;
    }
}
