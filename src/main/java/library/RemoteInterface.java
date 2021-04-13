package library;

import library.exceptions.DoubleMarkerException;
import library.exceptions.SnapshotInterruptException;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

// Creating Remote interface for our application 
interface RemoteInterface<MessageType> extends Remote {
    /**
     *
     * */
    void receiveMessage(String senderIp, int senderPort, MessageType message) throws RemoteException, NotBoundException, SnapshotInterruptException;

    /**
     *
     * */
    void receiveMarker(String senderIp, int senderPort, String initiatorIp, int initiatorPort, int id) throws RemoteException, DoubleMarkerException;

    /**
     *
     * */
    void addMeBack(String ip_address, int port) throws RemoteException, NotBoundException;

    /**
     *
     * */
    void removeMe(String ip_address, int port) throws RemoteException, SnapshotInterruptException;
}