package library;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SnapshotAndStorageTest {

    @Test
    public void TestEquals() {
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
    void writeFile() {

    }

    @Test
    void createFolder(){

    }
}

class MockState1 {
    String randomString;
    int randomInt;

    public MockState1(String randomString, int randomInt) {
        this.randomString = randomString;
        this.randomInt = randomInt;
    }
}

class MockState2 {
    char randomChar;
    double randomFloat;

    public MockState2(char randomChar, double randomFloat) {
        this.randomChar = randomChar;
        this.randomFloat = randomFloat;
    }
}