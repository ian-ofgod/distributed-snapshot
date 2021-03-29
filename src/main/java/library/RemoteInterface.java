package library;

import java.rmi.Remote;
import java.rmi.RemoteException;

// Creating Remote interface for our application 
interface RemoteInterface extends Remote {
    <MessageType> void receiveMessage(String senderIp, int senderPort, MessageType message)  throws RemoteException;
    void receiveMarker(String senderIp, int senderPort, String initiatorIp, int initiatorPort, int id)  throws RemoteException;
    void addMeBack(String ip_address, int port) throws RemoteException;
}