package library;

import java.util.ArrayList;
import java.util.Objects;

public class RemoteNode {
    String ip_address;
    int port;
    RemoteInterface remoteInterface; //the remote interface of the node
    ArrayList<Integer> markerIdsReceived = new ArrayList<>(); //holds the marker.id received from this remoteNode (for multiple concurrent distributed snapshots)

    public ArrayList<Integer> getMarkerIdsSent() {
        return markerIdsSent;
    }

    ArrayList<Integer> markerIdsSent = new ArrayList<>(); //holds the marker.id sent to this remoteNode (for multiple concurrent distributed snapshots)



    public RemoteNode(String ip_address, int port, RemoteInterface remoteInterface) {
        this.ip_address = ip_address;
        this.port = port;
        this.remoteInterface = remoteInterface;
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


    public ArrayList<Integer> getMarkerIdsReceived() {
        return markerIdsReceived;
    }


    public RemoteInterface getRemoteInterface() {
        return remoteInterface;
    }
}
