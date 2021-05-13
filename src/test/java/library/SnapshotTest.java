package library;

import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class SnapshotTest {

    @Test
    public void testEquals() {
        int id = 1;
        MockState1 state1 = new MockState1("bla",1);
        MockState2 state2 = new MockState2('c',2.0);


        Snapshot snapshot1 = new Snapshot(id,state1);
        Snapshot snapshot2 = new Snapshot(id,state2);
        Snapshot snapshot3 = new Snapshot(id+1,state2);

        assertEquals(snapshot1,snapshot2);
        assertNotEquals(snapshot2,snapshot3);

    }

    @Test
    void writeFileTest() {

        MockState1 state1 = new MockState1("bla",1);
        MockState2 state2 = new MockState2('c',2.0);
        MockMessage1 message1 = new MockMessage1('q',"Hello World");
        MockMessage2 message2 = new MockMessage2('q','z');

        Snapshot<MockState1,MockMessage1> snapshot1a = new Snapshot<>(1,state1);
        Snapshot<MockState1,MockMessage2> snapshot2a = new Snapshot<>(2,state1);
        Snapshot<MockState2,MockMessage1> snapshot3a = new Snapshot<>(3,state2);
        Snapshot<MockState2,MockMessage2> snapshot4a = new Snapshot<>(4,state2);

        Snapshot<MockState1,MockMessage1> snapshot1b = new Snapshot<>(5,state1);
        Snapshot<MockState1,MockMessage2> snapshot2b = new Snapshot<>(6,state1);
        Snapshot<MockState2,MockMessage1> snapshot3b = new Snapshot<>(7,state2);
        Snapshot<MockState2,MockMessage2> snapshot4b = new Snapshot<>(8,state2);


        Entity entity1 = new Entity("192.168.0.1",123);
        Entity entity2 = new Entity("192.168.0.2",456);
        Entity entity3 = new Entity("192.168.0.3",789);

        snapshot1a.messages.put(entity1, new ArrayList<>());
        snapshot1a.messages.put(entity2, new ArrayList<>());
        snapshot1a.messages.put(entity3, new ArrayList<>());

        snapshot2a.messages.put(entity1, new ArrayList<>());
        snapshot2a.messages.put(entity2, new ArrayList<>());
        snapshot2a.messages.put(entity3, new ArrayList<>());

        snapshot3a.messages.put(entity1, new ArrayList<>());
        snapshot3a.messages.put(entity2, new ArrayList<>());
        snapshot3a.messages.put(entity3, new ArrayList<>());

        snapshot4a.messages.put(entity1, new ArrayList<>());
        snapshot4a.messages.put(entity2, new ArrayList<>());
        snapshot4a.messages.put(entity3, new ArrayList<>());

        snapshot1b.messages.put(entity1, new ArrayList<>());
        snapshot1b.messages.put(entity2, new ArrayList<>());
        snapshot1b.messages.put(entity3, new ArrayList<>());

        snapshot2b.messages.put(entity1, new ArrayList<>());
        snapshot2b.messages.put(entity2, new ArrayList<>());
        snapshot2b.messages.put(entity3, new ArrayList<>());

        snapshot3b.messages.put(entity1, new ArrayList<>());
        snapshot3b.messages.put(entity2, new ArrayList<>());
        snapshot3b.messages.put(entity3, new ArrayList<>());

        snapshot4b.messages.put(entity1, new ArrayList<>());
        snapshot4b.messages.put(entity2, new ArrayList<>());
        snapshot4b.messages.put(entity3, new ArrayList<>());


        snapshot1a.messages.get(entity1).add(message1);
        snapshot1a.messages.get(entity2).add(message1);
        snapshot1a.messages.get(entity3).add(message1);

        snapshot2a.messages.get(entity1).add(message2);
        snapshot2a.messages.get(entity2).add(message2);
        snapshot2a.messages.get(entity3).add(message2);

        snapshot3a.messages.get(entity1).add(message1);
        snapshot3a.messages.get(entity2).add(message1);
        snapshot3a.messages.get(entity3).add(message1);

        snapshot4a.messages.get(entity1).add(message2);
        snapshot4a.messages.get(entity2).add(message2);
        snapshot4a.messages.get(entity3).add(message2);

        snapshot1b.messages.get(entity1).add(message1);
        snapshot1b.messages.get(entity2).add(message1);
        snapshot1b.messages.get(entity3).add(message1);

        snapshot2b.messages.get(entity1).add(message2);
        snapshot2b.messages.get(entity2).add(message2);
        snapshot2b.messages.get(entity3).add(message2);

        snapshot3b.messages.get(entity1).add(message1);
        snapshot3b.messages.get(entity2).add(message1);
        snapshot3b.messages.get(entity3).add(message1);

        snapshot4b.messages.get(entity1).add(message2);
        snapshot4b.messages.get(entity2).add(message2);
        snapshot4b.messages.get(entity3).add(message2);


        ArrayList<Snapshot<MockState1,MockMessage1>> runningSnapshots1 = new ArrayList<>();
        ArrayList<Snapshot<MockState1,MockMessage2>> runningSnapshots2 = new ArrayList<>();
        ArrayList<Snapshot<MockState2,MockMessage1>> runningSnapshots3 = new ArrayList<>();
        ArrayList<Snapshot<MockState2,MockMessage2>> runningSnapshots4 = new ArrayList<>();


        runningSnapshots1.add(snapshot1a);
        runningSnapshots2.add(snapshot2a);
        runningSnapshots3.add(snapshot3a);
        runningSnapshots4.add(snapshot4a);

        runningSnapshots1.add(snapshot1b);
        runningSnapshots2.add(snapshot2b);
        runningSnapshots3.add(snapshot3b);
        runningSnapshots4.add(snapshot4b);

        Storage.writeFile(runningSnapshots1,1);
        Storage.writeFile(runningSnapshots2,2);
        Storage.writeFile(runningSnapshots3,3);
        Storage.writeFile(runningSnapshots4,4);

    }



}

class MockState1 implements Serializable {
    String randomString;
    int randomInt;

    public MockState1(String randomString, int randomInt) {
        this.randomString = randomString;
        this.randomInt = randomInt;
    }
}

class MockState2 implements Serializable {
    char randomChar;
    double randomFloat;

    public MockState2(char randomChar, double randomFloat) {
        this.randomChar = randomChar;
        this.randomFloat = randomFloat;
    }
}

class MockMessage1 implements Serializable {

    char randomChar;
    String randomString;

    public MockMessage1(char randomChar, String randomString) {
        this.randomChar = randomChar;
        this.randomString = randomString;
    }

    @Override
    public String toString() {
        return "###############THIS IS MESSAGE 1####################";
    }
}

class MockMessage2 implements Serializable {
    char randomChar1;
    char randomChar2;

    public MockMessage2(char randomChar1, char randomChar2) {
        this.randomChar1 = randomChar1;
        this.randomChar2 = randomChar2;
    }

    @Override
    public String toString() {
        return "###############THIS IS MESSAGE 2####################";
    }
}
