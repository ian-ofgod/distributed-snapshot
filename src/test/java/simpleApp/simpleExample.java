package simpleApp;

import library.*;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

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
        node1.addConnection(InetAddress.getLocalHost().getHostAddress(),11112);

        node1.sendMessage(InetAddress.getLocalHost().getHostAddress(), 11112, new Message("Messaggio 1->2 che è stato processato da 2"));
        node2.sendMessage(InetAddress.getLocalHost().getHostAddress(), 11111, new Message("Messaggio 2->1 che è stato processato da 1"));

        node1.initiateSnapshot();
    }
}

class BasicApp1 implements AppConnector {
    @Override
    public void handleIncomingMessage(String senderIp, int senderPort, Object o) {
        System.out.println("BASIC APP 1: HANDLING THE MESSAGGEEEEOIHDAUHEF");
    }

    @Override
    public void handleNewConnection(String newConnectionIp, int newConnectionPort) {
        System.out.println("BASIC APP 1: Connection between me and "+newConnectionIp+":"+newConnectionPort+" was successfully added from remote");
    }
}

class BasicApp2 implements AppConnector {
    @Override
    public void handleIncomingMessage(String senderIp, int senderPort, Object o) {
        System.out.println("BASIC APP 2 HANDLING THE MESSAGE");
    }

    @Override
    public void handleNewConnection(String newConnectionIp, int newConnectionPort) {
        System.out.println("BASIC APP 2: Connection between me and "+newConnectionIp+":"+newConnectionPort+" was successfully added from remote");
    }
}

class Message implements Serializable {
    String message;
    Message(String msg){
        message=msg;
    }
}