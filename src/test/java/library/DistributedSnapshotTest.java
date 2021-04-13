package library;

import library.exceptions.RemoteNodeAlreadyPresent;
import library.exceptions.RemoteNodeNotFound;
import org.junit.Test;

import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class DistributedSnapshotTest {
    @Test
    public void simpleExample() throws NotBoundException, RemoteNodeAlreadyPresent, RemoteException, RemoteNodeNotFound {
        BasicApp<Message> basicApp1= new BasicApp<>();
        BasicApp<Message> basicApp2= new BasicApp<>();
        BasicApp<Message> basicApp3= new BasicApp<>();
        BasicApp<Message> basicApp4= new BasicApp<>();
        BasicApp<Message> basicApp5= new BasicApp<>();

        DistributedSnapshot<State,Message> node1 = new DistributedSnapshot<>(basicApp1,"localhost", 11111);
        DistributedSnapshot<State,Message> node2 = new DistributedSnapshot<>(basicApp2,"localhost", 11112);
        DistributedSnapshot<State,Message> node3 = new DistributedSnapshot<>(basicApp3,"localhost", 11113);
        DistributedSnapshot<State,Message> node4 = new DistributedSnapshot<>(basicApp4,"localhost", 11114);
        DistributedSnapshot<State,Message> node5 = new DistributedSnapshot<>(basicApp5,"localhost", 11115);

        /*     NETWORK STRUCTURE
               1---2---3 --- 4
                        \___ 5
         */
        node1.addConnection("localhost",11112); //node1 <--> node2
        node2.addConnection("localhost",11113); //node2 <--> node3
        node3.addConnection("localhost",11114); //node3 <--> node4
        node3.addConnection("localhost",11115); //node3 <--> node5

        node1.updateState(new State());
        node2.updateState(new State());
        node3.updateState(new State());
        node4.updateState(new State());
        node5.updateState(new State());

        node1.sendMessage("localhost", 11112, new Message("Message from 1 -> 2"));
        node2.sendMessage("localhost", 11113, new Message("Message from 2 -> 3"));
        node3.sendMessage("localhost", 11114, new Message("Message from 3 -> 4"));
        node3.sendMessage("localhost", 11115, new Message("Message from 3 -> 5"));

        basicApp2.state.messages.forEach(message -> System.out.println(message.message));

        assert(true);
    }

}

class BasicApp<MessageType> implements AppConnector<MessageType> {
    State state=new State();

    @Override
    public void handleIncomingMessage(String senderIp, int senderPort, MessageType o) {
        System.out.println("BASIC APP 1: HANDLING THE MESSAGE");
        state.messages.add((Message) o);
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

class Message implements Serializable {
    String message;
    Message(String msg){
        message=msg;
    }
}

class State implements Serializable {
    ArrayList<Message> messages = new ArrayList<>();
}