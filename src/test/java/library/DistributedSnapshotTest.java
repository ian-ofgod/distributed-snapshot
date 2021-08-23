package library;

import library.exceptions.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DistributedSnapshotTest {
    @Test
    public void simpleExample() throws InterruptedException, UnexpectedMarkerReceived, RestoreInProgress, DoubleMarkerException, NotInitialized, RemoteException {
        ArrayList<App<Message,State>> apps = new ArrayList<>();
        apps.add(new App<>("localhost", 11111));
        apps.add(new App<>("localhost", 11112));
        apps.add(new App<>("localhost", 11113));
        apps.add(new App<>("localhost", 11114));
        apps.add(new App<>("localhost", 11115));

        // app[i] initialize & app[i] set initial state
        apps.forEach((app)-> {
            try {
                app.init(app);
                app.snapshotLibrary.updateState(new State());
            } catch (AlreadyBoundException | RemoteException | AlreadyInitialized | RestoreInProgress | StateUpdateException e) {
                e.printStackTrace();
            }
        });

        // app[i] join network
        apps.forEach((app)-> {
            try {
                app.snapshotLibrary.joinNetwork(apps.get(0).hostname,apps.get(0).port);
            } catch (RemoteException | NotBoundException | NotInitialized e) {
                e.printStackTrace();
            }
        });

        // make all the apps send messages to each other randomly
        ExecutorService send= Executors.newCachedThreadPool();
        send.submit(()-> sendLoop(apps, 0));
        send.submit(()-> sendLoop(apps, 1));
        send.submit(()-> sendLoop(apps, 2));
        send.submit(()-> sendLoop(apps, 3));
        send.submit(()-> sendLoop(apps, 4));

        //start a snapshot from one of the nodes
        apps.get(2).snapshotLibrary.initiateSnapshot();

        //needed to allow all the nodes to save the snapshot and all threads to finish propagating the marker
        Thread.sleep(2000);

        assertTrue(true);
    }

    @Test
    public void simpleSnapshotRestore() throws UnexpectedMarkerReceived, RestoreInProgress, DoubleMarkerException, NotInitialized, RemoteException, InterruptedException, RestoreAlreadyInProgress, NotBoundException {
        ArrayList<App<Message,State>> apps = new ArrayList<>();
        apps.add(new App<>("localhost", 11119));
        apps.add(new App<>("localhost", 11118));
        apps.add(new App<>("localhost", 11117));

        // app[i] initialize & app[i] set initial state
        apps.forEach((app)-> {
            try {
                app.init(app);
                app.state=new State(app.port);
                app.snapshotLibrary.updateState(app.state);
            } catch (AlreadyBoundException | RemoteException | AlreadyInitialized | RestoreInProgress | StateUpdateException e) {
                e.printStackTrace();
            }
        });

        // app[i] join network
        apps.forEach((app)-> {
            try {
                app.snapshotLibrary.joinNetwork(apps.get(0).hostname,apps.get(0).port);
            } catch (RemoteException | NotBoundException | NotInitialized e) {
                e.printStackTrace();
            }
        });

        apps.get(1).snapshotLibrary.initiateSnapshot();
        Thread.sleep(2000);

        //at this point we have finished a snapshot
        //we will now modify the state of all the applications and then proceed to restore from snapshot
        apps.forEach((app)-> {
            try {
                app.state=new State(-1);
                app.snapshotLibrary.updateState(app.state);
            } catch (RestoreInProgress | StateUpdateException e) {
                e.printStackTrace();
            }
        });
        apps.forEach((app)-> {
            assertEquals(app.state, new State(-1),
                    "["+app.hostname+":"+app.port+"] State.appId="+app.state.appId);
            assertEquals(app.snapshotLibrary.remoteImplementation.currentState, new State(-1),
                    "["+app.hostname+":"+app.port+"] remoteImplementation.currentState.appId="+app.state.appId);
        });

        //we restore the snapshot that we made
        apps.get(0).snapshotLibrary.restoreLastSnapshot();
        Thread.sleep(2000);

        //at this point we should be able to see the same state of the previous setup
        //TODO: check also for same connections
        apps.forEach((app)-> {
            assertEquals(app.state, new State(app.port),
                    "["+app.hostname+":"+app.port+"] State.appId="+app.state.appId);
            assertEquals(app.snapshotLibrary.remoteImplementation.currentState, new State(app.port),
                    "["+app.hostname+":"+app.port+"] remoteImplementation.currentState.appId="+app.state.appId);
        });

    }

    private void sendLoop(ArrayList<App<Message,State>> apps, int index)  {
        App<Message,State> current=apps.get(index);
        int random_index = new Random().nextInt(apps.size());
        while(true){
            if(random_index!=index) {
                try {
                    current.snapshotLibrary.sendMessage(apps.get(random_index).hostname, apps.get(random_index).port,
                            new Message("MSG from ["+current.hostname+":"+current.port+"]"));
                    Thread.sleep(10);
                } catch (RemoteNodeNotFound | RemoteException | NotBoundException |
                        NotInitialized | SnapshotInterruptException | RestoreInProgress | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            random_index= new Random().nextInt(apps.size());
        }
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
        System.out.println("["+hostname+":"+port+"] adding new connection to "+newConnectionHostname+":"+newConnectionPort);
    }

    @Override
    public void handleRemoveConnection(String removeConnectionHostname, int removeConnectionPort) {

    }

    @Override
    public void handleRestoredState(StateType state) {
        this.state= (State) state;
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
    int appId=-1;

    State(){}

    State(int appId){
        this.appId= appId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return appId == state.appId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(appId);
    }
}