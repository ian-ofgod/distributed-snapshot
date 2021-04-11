package simpleApp;

import library.*;
import library.exceptions.DoubleMarkerException;
import library.exceptions.RemoteNodeNotFound;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class simpleExample {
    public static void main(String[] args) throws UnknownHostException {
        BasicApp1 basicApp1 = new BasicApp1();
        BasicApp2 basicApp2 = new BasicApp2();

        Node node1 = new Node(basicApp1, InetAddress.getLocalHost().getHostAddress(), 11111);
        Node node2 = new Node(basicApp2,InetAddress.getLocalHost().getHostAddress(), 11112);
        Node node3 = new Node(basicApp2,InetAddress.getLocalHost().getHostAddress(), 11113);

/*
        node1.addConnection(InetAddress.getLocalHost().getHostAddress(), 11112);
        node2.addConnection(InetAddress.getLocalHost().getHostAddress(), 11111);

        node1.addConnection(InetAddress.getLocalHost().getHostAddress(), 11113);
        node3.addConnection(InetAddress.getLocalHost().getHostAddress(), 11111);

        node3.addConnection(InetAddress.getLocalHost().getHostAddress(), 11112);
        node2.addConnection(InetAddress.getLocalHost().getHostAddress(), 11113);

 */
        try {
            node1.addConnection(InetAddress.getLocalHost().getHostAddress(),11112);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
        try {
            node1.sendMessage(InetAddress.getLocalHost().getHostAddress(), 11112, new Message("Messaggio 1->2 che è stato processato da 2"));
            node2.sendMessage(InetAddress.getLocalHost().getHostAddress(), 11111, new Message("Messaggio 2->1 che è stato processato da 1"));
        }catch (RemoteNodeNotFound | RemoteException e){
            e.printStackTrace();
        }


        try {
            node1.initiateSnapshot();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (DoubleMarkerException e) {
            e.printStackTrace();
        }

        System.out.println("Stopping node1");
        node1.stop();
        System.out.println("Stopped node1");

        System.out.println("Stopping node2");
        node2.stop();
        System.out.println("Stopped node2");
    }
}

class BasicApp1 implements AppConnector {
    @Override
    public void handleIncomingMessage(String senderIp, int senderPort, Object o) {
        System.out.println("BASIC APP 1: HANDLING THE MESSAGGEEEEOIHDAUHEF");
    }

    @Override
    public void handleNewConnection(String newConnectionIp, int newConnectionPort) {
        System.out.println("BASIC APP 1: Connection between me and "+newConnectionIp+":"+newConnectionPort+" was successfully ADDED from remote");
    }

    @Override
    public void handleRemoveConnection(String removeConnectionIp, int removeConnectionPort) {
        System.out.println("BASIC APP 1: Connection between me and "+removeConnectionIp+":"+removeConnectionPort+" was successfully REMOVED from remote");
    }
}

class BasicApp2 implements AppConnector {
    @Override
    public void handleIncomingMessage(String senderIp, int senderPort, Object o) {
        System.out.println("BASIC APP 2 HANDLING THE MESSAGE");
    }

    @Override
    public void handleNewConnection(String newConnectionIp, int newConnectionPort) {
        System.out.println("BASIC APP 2: Connection between me and "+newConnectionIp+":"+newConnectionPort+" was successfully ADDED from remote");
    }

    @Override
    public void handleRemoveConnection(String removeConnectionIp, int removeConnectionPort) {
        System.out.println("BASIC APP 2: Connection between me and "+removeConnectionIp+":"+removeConnectionPort+" was successfully REMOVED from remote");
    }
}

class Message implements Serializable {
    String message;
    Message(String msg){
        message=msg;
    }
}