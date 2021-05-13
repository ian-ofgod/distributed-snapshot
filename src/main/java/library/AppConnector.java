package library;

import library.exceptions.StateUpdateException;

/**
 * This interface must be implemented by the application in order to use the library.
 * It is composed of the methods that will be triggered by the library when an new event occurs.
 * @param <MessageType> this is the type that will be exchanged as a message between nodes
 */
public interface AppConnector<MessageType> {
    /**
     * This is the method for the application to handle a new incoming message from another node on the network.
     * @param senderHostname the hostname of the sender
     * @param senderPort the port assigned to the RMI registry of the sender
     * @param o the message that the remote node sent
     */
    void handleIncomingMessage(String senderHostname, int senderPort, MessageType o);

    /**
     * This is the method for the application to handle a new connection from another node of the network.
     * When creating a link the library establish a bi-directional communication between the two nodes, so this function
     * is called when the other party decides to add a connection with us.
     * @param newConnectionHostname the hostname of the node requesting the connection
     * @param newConnectionPort the port assigned to the RMI registry of the new connection
     */
    void handleNewConnection(String newConnectionHostname, int newConnectionPort);

    /**
     * This is the method for the application to handle the removal of a connection asked from a another node of the network.
     * When creating a link the library establish a bi-directional communication between the two nodes, so this function
     * is called when the other party decides to remove the connection.
     * @param removeConnectionHostname the hostname of the node requesting the removal of the connection
     * @param removeConnectionPort the port assigned to the RMI registry of the node requesting the removal of the connection
     */
    void handleRemoveConnection(String removeConnectionHostname, int removeConnectionPort);
}
