package library;

import library.exceptions.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DistributedSnapshotTest {
    @Test
    public void simpleExample() throws InterruptedException, IOException, UnexpectedMarkerReceived, RestoreInProgress, DoubleMarkerException, NotInitialized {
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

        // each app will start sending messages to each other randomly
        ExecutorService executorService= Executors.newCachedThreadPool();
        executorService.submit(()-> sendLoop(apps, 0));
        executorService.submit(()-> sendLoop(apps, 1));
        executorService.submit(()-> sendLoop(apps, 2));
        executorService.submit(()-> sendLoop(apps, 3));
        executorService.submit(()-> sendLoop(apps, 4));

        Thread.sleep(100); //let them exchange some messages
        printAppsState(apps); //print the number of messages received

        // start snapshot
        apps.get(2).snapshotLibrary.initiateSnapshot();
        Thread.sleep(100);  // let them exchange some messages
        printAppsState(apps); // print the number of messages received

        executorService.shutdownNow();
        if (!executorService.awaitTermination(100, TimeUnit.MILLISECONDS)) {
            System.out.println("Still waiting...");
            System.exit(0);
        }
        System.out.println("Exiting normally...");

        Storage.cleanStorageFolder();
    }

    @Test
    public void simpleSnapshotRestore() throws RestoreInProgress, IOException, InterruptedException, RestoreAlreadyInProgress, NotBoundException, RestoreNotPossible, ClassNotFoundException, UnexpectedMarkerReceived, DoubleMarkerException, NotInitialized {
        ArrayList<App<Message,State>> apps = new ArrayList<>();
        apps.add(new App<>("localhost", 11121));
        apps.add(new App<>("localhost", 11122));
        //apps.add(new App<>("localhost", 11123));

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

        // start sending messages
        ExecutorService executorService= Executors.newCachedThreadPool();
        executorService.submit(()-> sendLoop(apps, 0));
        executorService.submit(()-> sendLoop(apps, 1));
        executorService.submit(()-> sendLoop(apps, 2));


        // let localhost:11118 start the snapshot
        apps.get(1).snapshotLibrary.initiateSnapshot();
        Thread.sleep(500); // let the snapshot finish

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
            System.out.println(app.state.appId);
            assertEquals(app.state, new State(-1),
                    "["+app.hostname+":"+app.port+"] State.appId="+app.state.appId);
            assertEquals(app.snapshotLibrary.remoteImplementation.currentState, new State(-1),
                    "["+app.hostname+":"+app.port+"] remoteImplementation.currentState.appId="+app.state.appId);
        });

        //we restore the snapshot that we made
        apps.get(0).snapshotLibrary.restoreLastSnapshot();
        Thread.sleep(200);

        //at this point we should be able to see the same state of the previous setup
        apps.forEach((app)-> {
            System.out.println(app.state.appId);
            assertEquals(app.state, new State(app.port),
                    "["+app.hostname+":"+app.port+"] State.appId="+app.state.appId);
            assertEquals(app.snapshotLibrary.remoteImplementation.currentState, new State(app.port),
                    "["+app.hostname+":"+app.port+"] remoteImplementation.currentState.appId="+app.state.appId);
        });

        executorService.shutdownNow();
        if (!executorService.awaitTermination(100, TimeUnit.MILLISECONDS)) {
            System.out.println("Still waiting...");
            System.exit(0);
        }
        System.out.println("Exiting normally...");

        Storage.cleanStorageFolder();

    }

    @Test
    public void restoreSnapshotWithRemovedNode() throws UnexpectedMarkerReceived, RestoreInProgress, DoubleMarkerException, NotInitialized, IOException, InterruptedException, RestoreAlreadyInProgress, NotBoundException, OperationForbidden, SnapshotInterruptException, RestoreNotPossible, ClassNotFoundException {
        ArrayList<App<Message, State>> apps = new ArrayList<>();
        apps.add(new App<>("localhost", 11131));
        apps.add(new App<>("localhost", 11132));
        apps.add(new App<>("localhost", 11133));
        apps.add(new App<>("localhost", 11134));


        // app[i] initialize & app[i] set initial state
        apps.forEach((app) -> {
            try {
                app.init(app);
                app.state = new State(app.port);
                app.snapshotLibrary.updateState(app.state);
            } catch (AlreadyBoundException | RemoteException | AlreadyInitialized | RestoreInProgress | StateUpdateException e) {
                e.printStackTrace();
            }
        });

        // app[i] join network
        apps.forEach((app) -> {
            try {
                app.snapshotLibrary.joinNetwork(apps.get(0).hostname, apps.get(0).port);
            } catch (RemoteException | NotBoundException | NotInitialized e) {
                e.printStackTrace();
            }
        });

        // make all the apps send messages to each other randomly
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.submit(()-> sendLoop(apps, 0));
        executorService.submit(()-> sendLoop(apps, 1));
        executorService.submit(()-> sendLoop(apps, 2));
        executorService.submit(()-> sendLoop(apps, 3));

        //make a snapshot
        apps.get(0).snapshotLibrary.initiateSnapshot();
        Thread.sleep(500);

        //we disconnect one app from the network
        apps.get(2).snapshotLibrary.disconnect();
        Thread.sleep(500);


        //check that all the apps have removed that node from the network
        apps.forEach((app)->{
            if(!app.equals(apps.get(2))){
                assertNull(app.snapshotLibrary.remoteImplementation.getRemoteNode(apps.get(2).hostname, apps.get(2).port));
                assertNull(app.getEntity(apps.get(2).hostname, apps.get(2).port));
            }
        });
        for (RemoteNode<Message> remoteNode : apps.get(3).snapshotLibrary.remoteImplementation.remoteNodes) {
            System.out.println("Before: "+remoteNode.hostname+":"+remoteNode.port);
        }

        //we restore the snapshot that we made
        apps.get(1).snapshotLibrary.restoreLastSnapshot();
        Thread.sleep(200);

        // after the snapshot is restored we should be able to see again the connection to apps.get(2)
        apps.forEach((app)->{
            if(!app.equals(apps.get(2))){
                assertNotNull(app.snapshotLibrary.remoteImplementation.getRemoteNode(apps.get(2).hostname, apps.get(2).port));
                assertNotNull(app.getEntity(apps.get(2).hostname, apps.get(2).port));
            }
        });
        for (RemoteNode<Message> remoteNode : apps.get(3).snapshotLibrary.remoteImplementation.remoteNodes) {
            System.out.println("After: "+remoteNode.hostname+":"+remoteNode.port);
        }

        executorService.shutdownNow();
        if (!executorService.awaitTermination(100, TimeUnit.MILLISECONDS)) {
            System.out.println("Still waiting...");
            System.exit(0);
        }
        System.out.println("Exiting normally...");

        Storage.cleanStorageFolder();

    }

    @Test
    public void restoreNotPossibleNodeNotAvailable() throws UnexpectedMarkerReceived, RestoreInProgress, DoubleMarkerException, NotInitialized, IOException, InterruptedException, OperationForbidden, SnapshotInterruptException, RestoreAlreadyInProgress, NotBoundException, RestoreNotPossible {
        ArrayList<App<Message, State>> apps = new ArrayList<>();
        apps.add(new App<>("localhost", 11141));
        apps.add(new App<>("localhost", 11142));
        apps.add(new App<>("localhost", 11143));
        apps.add(new App<>("localhost", 11144));


        // app[i] initialize & app[i] set initial state
        apps.forEach((app) -> {
            try {
                app.init(app);
                app.state = new State(app.port);
                app.snapshotLibrary.updateState(app.state);
            } catch (AlreadyBoundException | RemoteException | AlreadyInitialized | RestoreInProgress | StateUpdateException e) {
                e.printStackTrace();
            }
        });

        // app[i] join network
        apps.forEach((app) -> {
            try {
                app.snapshotLibrary.joinNetwork(apps.get(0).hostname, apps.get(0).port);
            } catch (RemoteException | NotBoundException | NotInitialized e) {
                e.printStackTrace();
            }
        });

        // make all the apps send messages to each other randomly
        ExecutorService executorService= Executors.newCachedThreadPool();
        executorService.submit(()-> sendLoop(apps, 0));
        executorService.submit(()-> sendLoop(apps, 1));
        executorService.submit(()-> sendLoop(apps, 2));
        executorService.submit(()-> sendLoop(apps, 3));

        apps.get(0).snapshotLibrary.initiateSnapshot(); // localhost:11141
        Thread.sleep(200);
        printAppsState(apps);


        //we disconnect one app from the network
        apps.get(2).snapshotLibrary.disconnect(); // localhost:11143
        apps.get(2).snapshotLibrary.stop();


        //we check that the restore is not possible
        assertThrows(RestoreNotPossible.class, () -> apps.get(1).snapshotLibrary.restoreLastSnapshot());
        Thread.sleep(100);

        printAppsState(apps);

        executorService.shutdownNow();
        if (!executorService.awaitTermination(100, TimeUnit.MILLISECONDS)) {
            System.out.println("Still waiting...");
            System.exit(0);
        }
        System.out.println("Exiting normally...");

        Storage.cleanStorageFolder();


    }

    @Test
    public void restoreWithMultipleSnapshotAvailable() throws IOException, NotInitialized, RestoreInProgress, DoubleMarkerException, InterruptedException, UnexpectedMarkerReceived, RestoreNotPossible, NotBoundException, RestoreAlreadyInProgress, ClassNotFoundException {
        ArrayList<App<Message, State>> apps = new ArrayList<>();
        apps.add(new App<>("localhost", 11251));
        apps.add(new App<>("localhost", 11252));
        apps.add(new App<>("localhost", 11253));
        apps.add(new App<>("localhost", 11254));

        // app[i] initialize & app[i] set initial state
        apps.forEach((app) -> {
            try {
                app.init(app);
                app.state = new State(app.port);
                app.snapshotLibrary.updateState(app.state);
            } catch (AlreadyBoundException | RemoteException | AlreadyInitialized | RestoreInProgress | StateUpdateException e) {
                e.printStackTrace();
            }
        });

        // app[i] join network
        apps.forEach((app) -> {
            try {
                app.snapshotLibrary.joinNetwork(apps.get(0).hostname, apps.get(0).port);
            } catch (RemoteException | NotBoundException | NotInitialized e) {
                e.printStackTrace();
            }
        });

        // make all the apps send messages to each other randomly
        ExecutorService executorService= Executors.newCachedThreadPool();
        executorService.submit(()-> sendLoop(apps, 0));
        executorService.submit(()-> sendLoop(apps, 1));
        executorService.submit(()-> sendLoop(apps, 2));
        executorService.submit(()-> sendLoop(apps, 3));

        apps.get(0).snapshotLibrary.initiateSnapshot(); // localhost:11151
        Thread.sleep(500);


        apps.get(0).snapshotLibrary.initiateSnapshot(); // localhost:11151
        Thread.sleep(500);


        printAppsState(apps); //state to be seen in the snapshot


        //we restore the snapshot that we made
        apps.get(0).snapshotLibrary.restoreLastSnapshot();
        Thread.sleep(500);

        printAppsState(apps);

        //todo: assert

        executorService.shutdownNow();
        if (!executorService.awaitTermination(100, TimeUnit.MILLISECONDS)) {
            System.out.println("Still waiting...");
            System.exit(0);
        }
        System.out.println("Exiting normally...");

        Storage.cleanStorageFolder();


    }

    private void printAppsState(ArrayList<App<Message,State>> apps){
        System.out.println("----------------------------------");
        System.out.println("STATUS:");
        apps.forEach((app)->{
            System.out.println("["+app.hostname+":"+app.port+"] -> #MSG="+app.state.messages.size());
        });
        System.out.println("----------------------------------");
    }

    private void sendLoop(ArrayList<App<Message,State>> apps, int index) {
        App<Message,State> current=apps.get(index);
        App<Message,State> send_to;

        try {
            while (true) {
                send_to = apps.get(new Random().nextInt(apps.size())); // get a random app to send the message to
                if (current != null && send_to != null && current.snapshotLibrary.remoteImplementation.remoteNodes.size() != 0) { // the thread that runs sendLoop on a removed node will be doing nothing
                    try {
                        System.out.println("FROM [" + current.port + "] MSG TO " + send_to.port);
                        current.snapshotLibrary.sendMessage(send_to.hostname, send_to.port,
                                new Message("MSG from [" + current.hostname + ":" + current.port + "]"));
                    } catch (RemoteException | NotBoundException | NotInitialized | SnapshotInterruptException e) {
                        System.out.println("ANOTHER TYPE OF ERROR");
                    } catch (RestoreInProgress e) {
                        System.out.println("WAITING END OF RESTORE");
                    } catch (RemoteNodeNotFound e) {
                        System.out.println("[" + current.port + "] TRYING TO SEND A MESSAGE TO REMOVED NODE: " + send_to.port);
                    }
                }
                Thread.sleep(42); // Answer to the Ultimate Question of Life, the Universe, and Everything.
            }
        }catch (InterruptedException e) {
            System.out.println("sending thread exiting");
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

    Entity getEntity(String hostname, int port){
        for (Entity entity : connections) {
            if(entity.getIpAddress().equals(hostname)&& entity.getPort()==port)
                return entity;
        }
        return null;
    }

    @Override
    public void handleIncomingMessage(String senderHostname, int senderPort, MessageType o) {
        state.messages.add((Message) o);
        try {
            snapshotLibrary.updateState(state);
        } catch (StateUpdateException | RestoreInProgress e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleNewConnection(String newConnectionHostname, int newConnectionPort) {
        connections.add(new Entity(newConnectionHostname, newConnectionPort));
    }

    @Override
    public void handleRemoveConnection(String removeConnectionHostname, int removeConnectionPort) {
        connections.remove(new Entity(removeConnectionHostname,removeConnectionPort));
    }

    @Override
    public void handleRestoredState(StateType state) {
        this.state= (State) state;
    }

    @Override
    public void handleRestoredConnections(ArrayList<Entity> connections) {
        this.connections=new ArrayList<>(connections);
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