package library;

import java.rmi.Remote;
import java.rmi.RemoteException;

// Creating Remote interface for our application 
public interface RemoteInterface extends Remote {
    <MessageType> void receiveMessage(MessageType message)  throws RemoteException;
    void addMeBack(String ip_address, int port);
}