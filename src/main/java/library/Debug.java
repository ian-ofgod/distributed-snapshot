package library;

import library.exceptions.*;
import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class Debug {
    public static void main(String[] args) throws AlreadyBoundException, RemoteException, AlreadyInitialized, NotBoundException, NotInitialized, StateUpdateException, RestoreInProgress, UnexpectedMarkerReceived, DoubleMarkerException, InterruptedException {
        App<Message, State> app1 = new App<>("localhost",11111);
        App<Message, State> app2 = new App<>("localhost",11112);
        App<Message, State> app3 = new App<>("localhost",11113);
        App<Message, State> app4 = new App<>("localhost",11114);
        App<Message, State> app5 = new App<>("localhost",11115);

        app1.init(app1);
        app2.init(app2);
        app3.init(app3);
        app4.init(app4);
        app5.init(app5);

        // node1 is the gateway in this situation
        app2.connections=app2.snapshotLibrary.joinNetwork(app1.hostname, app1.port);
        app3.connections=app3.snapshotLibrary.joinNetwork(app1.hostname, app1.port);
        app4.connections=app4.snapshotLibrary.joinNetwork(app1.hostname, app1.port);
        app5.connections=app5.snapshotLibrary.joinNetwork(app1.hostname, app1.port);


        app1.snapshotLibrary.updateState(new State());
        app2.snapshotLibrary.updateState(new State());
        app3.snapshotLibrary.updateState(new State());
        app4.snapshotLibrary.updateState(new State());
        app5.snapshotLibrary.updateState(new State());

        Thread sending = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        app1.snapshotLibrary.sendMessage(app2.hostname, app2.port, new Message("Message from 1 -> 2"));
                        app2.snapshotLibrary.sendMessage(app1.hostname, app1.port, new Message("Message from 2 -> 1"));
                        app3.snapshotLibrary.sendMessage(app1.hostname, app1.port, new Message("Message from 3 -> 1"));
                        app3.snapshotLibrary.sendMessage(app5.hostname, app5.port, new Message("Message1 from 3 -> 5"));
                        app3.snapshotLibrary.sendMessage(app5.hostname, app5.port, new Message("Message2 from 3 -> 5"));
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        });
        sending.start();

        app3.snapshotLibrary.initiateSnapshot();

        sending.join();
    }
}


class App<MessageType, StateType> implements AppConnector<MessageType, StateType>{
    State state = new State();
    ArrayList<Entity> connections = new ArrayList<>();
    DistributedSnapshot<State,Message> snapshotLibrary = new DistributedSnapshot<>();
    String hostname;
    int port;

    App(String localhost, int port){
        this.hostname=localhost;
        this.port=port;
    }

    void init(AppConnector<Message, State> appConnector) throws AlreadyBoundException, RemoteException, AlreadyInitialized {
        snapshotLibrary.init(hostname, port, appConnector);
    }

    @Override
    public void handleIncomingMessage(String senderHostname, int senderPort, MessageType o) {
        state.messages.add((Message) o);
    }

    @Override
    public void handleNewConnection(String newConnectionHostname, int newConnectionPort) {
        connections.add(new Entity(newConnectionHostname, newConnectionPort));
    }

    @Override
    public void handleRemoveConnection(String removeConnectionHostname, int removeConnectionPort) {

    }

    @Override
    public void handleRestoredState(StateType state) {

    }

    @Override
    public void handleRestoredConnections(ArrayList<Entity> connections) {

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
