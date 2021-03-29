package library;

public interface AppConnector {
    void handleIncomingMessage(String senderIp, int senderPort, Object o);
    void handleNewConnection(String newConnectionIp, int newConnectionPort);
}
