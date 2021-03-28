package library;

public interface AppConnector {
    void handleIncomingMessage(String senderIp, int senderPort, Object o);
    //TODO: handleNewConnection()
}
