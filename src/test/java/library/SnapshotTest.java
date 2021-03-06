package library;


import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SnapshotTest {
    @Test
    void writeFileTest() throws IOException, ClassNotFoundException {

        MockState1 state1 = new MockState1("bla",1);
        MockState2 state2 = new MockState2('c',2.0);
        MockMessage1 message1 = new MockMessage1('q',"Hello World");
        MockMessage2 message2 = new MockMessage2('q','z');

        Snapshot<MockState1,MockMessage1> snapshot1a = new Snapshot<>("192.168.0.1123".hashCode(),state1);
        Snapshot<MockState1,MockMessage2> snapshot2a = new Snapshot<>("192.168.0.1124".hashCode(),state1);
        Snapshot<MockState2,MockMessage1> snapshot3a = new Snapshot<>("192.168.0.1125".hashCode(),state2);
        Snapshot<MockState2,MockMessage2> snapshot4a = new Snapshot<>("192.168.0.1126".hashCode(),state2);

        Snapshot<MockState1,MockMessage1> snapshot1b = new Snapshot<>("192.168.0.1127".hashCode(),state1);
        Snapshot<MockState1,MockMessage2> snapshot2b = new Snapshot<>("192.168.0.1128".hashCode(),state1);
        Snapshot<MockState2,MockMessage1> snapshot3b = new Snapshot<>("192.168.0.1129".hashCode(),state2);
        Snapshot<MockState2,MockMessage2> snapshot4b = new Snapshot<>("192.168.0.1130".hashCode(),state2);


        Entity entity1 = new Entity("192.168.0.1",123);
        Entity entity2 = new Entity("192.168.0.2",456);
        Entity entity3 = new Entity("192.168.0.3",789);


        snapshot1a.connectedNodes.add(entity1);
        snapshot1a.connectedNodes.add(entity2);
        snapshot1a.connectedNodes.add(entity3);

        snapshot1a.messages.add(new Envelope<>(entity1, message1));
        snapshot1a.messages.add(new Envelope<>(entity2, message1));
        snapshot1a.messages.add(new Envelope<>(entity3, message1));

        snapshot2a.messages.add(new Envelope<>(entity1,message2));
        snapshot2a.messages.add(new Envelope<>(entity2,message2));
        snapshot2a.messages.add(new Envelope<>(entity3,message2));

        snapshot3a.messages.add(new Envelope<>(entity1,message1));
        snapshot3a.messages.add(new Envelope<>(entity2,message1));
        snapshot3a.messages.add(new Envelope<>(entity3,message1));

        snapshot4a.messages.add(new Envelope<>(entity1,message2));
        snapshot4a.messages.add(new Envelope<>(entity2,message2));
        snapshot4a.messages.add(new Envelope<>(entity3,message2));

        snapshot1b.messages.add(new Envelope<>(entity1,message1));
        snapshot1b.messages.add(new Envelope<>(entity2,message1));
        snapshot1b.messages.add(new Envelope<>(entity3,message1));

        snapshot2b.messages.add(new Envelope<>(entity1,message2));
        snapshot2b.messages.add(new Envelope<>(entity2,message2));
        snapshot2b.messages.add(new Envelope<>(entity3,message2));

        snapshot3b.messages.add(new Envelope<>(entity1,message1));
        snapshot3b.messages.add(new Envelope<>(entity2,message1));
        snapshot3b.messages.add(new Envelope<>(entity3,message1));

        snapshot4b.messages.add(new Envelope<>(entity1,message2));
        snapshot4b.messages.add(new Envelope<>(entity2,message2));
        snapshot4b.messages.add(new Envelope<>(entity3,message2));


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


        Storage.writeFile(runningSnapshots1,"192.168.0.1123".hashCode(), "localhost", 0); // saves snapshot1a
        Storage.writeFile(runningSnapshots2,"192.168.0.1124".hashCode(), "localhost", 0); // saves snapshot2a
        Storage.writeFile(runningSnapshots3,"192.168.0.1125".hashCode(), "localhost", 0); // saves snapshot3a
        Storage.writeFile(runningSnapshots4,"192.168.0.1126".hashCode(), "localhost", 0); // saves snapshot4a
        Storage.writeFile(runningSnapshots1,"192.168.0.1127".hashCode(), "localhost", 0); // saves snapshot1b
        Storage.writeFile(runningSnapshots2,"192.168.0.1128".hashCode(), "localhost", 0); // saves snapshot2b
        Storage.writeFile(runningSnapshots3,"192.168.0.1129".hashCode(), "localhost", 0); // saves snapshot3b
        Storage.writeFile(runningSnapshots4,"192.168.0.1130".hashCode(), "localhost", 0); // saves snapshot4b


        Snapshot<MockState1,MockMessage1> readSnap1 = Storage.readFile("192.168.0.1123".hashCode(), "localhost",0);
        Snapshot<MockState1,MockMessage2> readSnap2 = Storage.readFile("192.168.0.1124".hashCode(), "localhost",0);
        Snapshot<MockState2,MockMessage1> readSnap3 = Storage.readFile("192.168.0.1125".hashCode(), "localhost",0);
        Snapshot<MockState2,MockMessage2> readSnap4 = Storage.readFile("192.168.0.1126".hashCode(), "localhost",0);
        Snapshot<MockState1,MockMessage1> readSnap5 = Storage.readFile("192.168.0.1127".hashCode(), "localhost",0);
        Snapshot<MockState1,MockMessage2> readSnap6 = Storage.readFile("192.168.0.1128".hashCode(), "localhost",0);
        Snapshot<MockState2,MockMessage1> readSnap7 = Storage.readFile("192.168.0.1129".hashCode(), "localhost",0);
        Snapshot<MockState2,MockMessage2> readSnap8 = Storage.readFile("192.168.0.1130".hashCode(), "localhost",0);

        /* we must test state.equals() and messages.equals() separately,
        because we use Snapshot.equals() with just the snapshot ID
        to check if a snapshot is already running */

        assertEquals(snapshot1a.state, readSnap1.state);
        assertEquals(snapshot2a.state, readSnap2.state);
        assertEquals(snapshot3a.state, readSnap3.state);
        assertEquals(snapshot4a.state, readSnap4.state);
        assertEquals(snapshot1b.state, readSnap5.state);
        assertEquals(snapshot2b.state, readSnap6.state);
        assertEquals(snapshot3b.state, readSnap7.state);
        assertEquals(snapshot4b.state, readSnap8.state);

        assertEquals(snapshot1a.messages, readSnap1.messages);
        assertEquals(snapshot2a.messages, readSnap2.messages);
        assertEquals(snapshot3a.messages, readSnap3.messages);
        assertEquals(snapshot4a.messages, readSnap4.messages);
        assertEquals(snapshot1b.messages, readSnap5.messages);
        assertEquals(snapshot2b.messages, readSnap6.messages);
        assertEquals(snapshot3b.messages, readSnap7.messages);
        assertEquals(snapshot4b.messages, readSnap8.messages);

        //test for the connected nodes
        assertEquals(snapshot1a.connectedNodes, readSnap1.connectedNodes);
    }




}



class MockState1 implements Serializable {

    @Serial
    private static final long serialVersionUID = 6529685098267757690L;

    String randomString;
    int randomInt;

    public MockState1(String randomString, int randomInt) {
        this.randomString = randomString;
        this.randomInt = randomInt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockState1 that = (MockState1) o;
        return randomInt == that.randomInt && randomString.equals(that.randomString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(randomString, randomInt);
    }
}

class MockState2 implements Serializable {

    @Serial
    private static final long serialVersionUID = 6529685098267757690L;

    char randomChar;
    double randomFloat;

    public MockState2(char randomChar, double randomFloat) {
        this.randomChar = randomChar;
        this.randomFloat = randomFloat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockState2 that = (MockState2) o;
        return randomChar == that.randomChar && Double.compare(that.randomFloat, randomFloat) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(randomChar, randomFloat);
    }
}

class MockMessage1 implements Serializable {

    @Serial
    private static final long serialVersionUID = 6529685098267757690L;

    char randomChar;
    String randomString;

    public MockMessage1(char randomChar, String randomString) {
        this.randomChar = randomChar;
        this.randomString = randomString;
    }

    @Override
    public String toString() {
        return "#MSG1#";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockMessage1 that = (MockMessage1) o;
        return randomChar == that.randomChar && Objects.equals(randomString, that.randomString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(randomChar, randomString);
    }
}

class MockMessage2 implements Serializable {

    @Serial
    private static final long serialVersionUID = 6529685098267757690L;

    char randomChar1;
    char randomChar2;

    public MockMessage2(char randomChar1, char randomChar2) {
        this.randomChar1 = randomChar1;
        this.randomChar2 = randomChar2;
    }

    @Override
    public String toString() {
        return "#MSG2#";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockMessage2 that = (MockMessage2) o;
        return randomChar1 == that.randomChar1 && randomChar2 == that.randomChar2;
    }

    @Override
    public int hashCode() {
        return Objects.hash(randomChar1, randomChar2);
    }
}
