package library;

import java.io.Serializable;
import java.util.Objects;

/**
 * Class to uniquely identify a sender entity: it wraps the ipAddress and the port
 * where a message is coming from, making it easier to store the received messages
 * in an HashMap with the sender Entity as a unique key.
 */
public class Entity implements Serializable { //Serializable needed to save ConnectedNodes on disk
    /**
     * ipAddress string associated to this entity
     */
    private String ipAddress;

    /**
     * Port number associated to this entity
     */
    private int port;

    /**
     * Sole constructor to create an Entity object
     * starting from an IP address and a port number
     *
     * @param ip_address the Entity ip address
     * @param port       the Entity port
     */
    public Entity(String ip_address, int port) {
        this.ipAddress = ip_address;
        this.port = port;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return ipAddress + ':' + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return port == entity.port && Objects.equals(ipAddress, entity.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress, port);
    }
}
