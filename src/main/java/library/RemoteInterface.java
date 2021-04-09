package library;

import library.exceptions.DoubleMarkerException;
import library.exceptions.SnapshotInterruptException;

import java.rmi.Remote;
import java.rmi.RemoteException;

// Creating Remote interface for our application 
interface RemoteInterface extends Remote {
    <MessageType> void receiveMessage(String senderIp, int senderPort, MessageType message)  throws RemoteException;
    void receiveMarker(String senderIp, int senderPort, String initiatorIp, int initiatorPort, int id) throws RemoteException, DoubleMarkerException;
    void addMeBack(String ip_address, int port) throws RemoteException;
    void removeMe(String ip_address, int port) throws RemoteException, SnapshotInterruptException;
}