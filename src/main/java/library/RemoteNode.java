package library;

import java.util.ArrayList;
import java.util.Objects;

public class RemoteNode {
    String ip_address;
    int port;
    RemoteInterface remoteInterface; //the remote interface of the node
    ArrayList<Integer> snapshotIdsReceived = new ArrayList<>(); //holds the marker.id received from this remoteNode (for multiple concurrent distributed snapshots)
    ArrayList<Integer> snapshotIdsSent = new ArrayList<>(); //holds the marker.id sent to this remoteNode (for multiple concurrent distributed snapshots)



    public RemoteNode(String ip_address, int port, RemoteInterface remoteInterface) {
        this.ip_address = ip_address;
        this.port = port;
        this.remoteInterface = remoteInterface;
    }


    public ArrayList<Integer> getSnapshotIdsSent() {
        return snapshotIdsSent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteNode that = (RemoteNode) o;
        return port == that.port && Objects.equals(ip_address, that.ip_address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip_address, port);
    }

    public String getIp_address() {
        return ip_address;
    }

    public int getPort() {
        return port;
    }


    public ArrayList<Integer> getSnapshotIdsReceived() {
        return snapshotIdsReceived;
    }


    public RemoteInterface getRemoteInterface() {
        return remoteInterface;
    }
}
