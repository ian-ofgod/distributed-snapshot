package library;

import library.exceptions.SnapshotInterruptException;
import library.exceptions.StateUpdateException;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class RemoteImplementationTest {

    @Test
    void receiveMarker() {
    }

    @Test
    void receiveMessageWithRunningSnapshot() {
        RemoteImplementation<String,String> rm = new RemoteImplementation<>();
        try {
            rm.appConnector = new MockApp<>();
            rm.remoteNodes.add(new RemoteNode<>("localhost", 12345, new RemoteImplementation<>()));
            rm.runningSnapshots.add(new Snapshot<>(1, "MockState"));
            rm.receiveMessage("localhost",12345, "MockMessage");

            assert(rm.runningSnapshots.get(0).messages.get(new Entity("localhost",12345)).contains("MockMessage"));
        } catch (RemoteException | NotBoundException | SnapshotInterruptException | StateUpdateException e) {
            e.printStackTrace(); //TODO: remove?
        }

    }
}

class MockApp<MessageType> implements AppConnector<MessageType>{
    @Override
    public void handleIncomingMessage(String senderHostname, int senderPort, MessageType o){
    }

    @Override
    public void handleNewConnection(String newConnectionHostname, int newConnectionPort) {

    }

    @Override
    public void handleRemoveConnection(String removeConnectionHostname, int removeConnectionPort) {

    }
}

class MockMessage implements Serializable {
    String message;
    MockMessage(String msg){
        message=msg;
    }
}

class MockState implements Serializable {
    String message;
    MockState(String msg){
        message=msg;
    }
}