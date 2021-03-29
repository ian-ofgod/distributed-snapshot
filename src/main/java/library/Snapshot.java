package library;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class Snapshot {
    // TODO: here we assume that all snapshots have CURRENT and INCOMING fields only

    private int currentAmount;
    private int incomingAmount;
    private int snapshotIdentifier;
    private final Set<Integer> markersNotReceived = new HashSet<>();


    //TODO: this constructor needs to be removed once we get the Snapshot from nod
    public Snapshot() {

    }

    public Snapshot(int currentAmount, int incomingAmount, int snapshotIdentifier) {
        this.currentAmount = currentAmount;
        this.incomingAmount = incomingAmount;
        this.snapshotIdentifier = snapshotIdentifier;
    }

    public void startSnapshotRecording(int nodeId, int balance, Map<Integer, String> nodes) {
        snapshotIdentifier++;
        currentAmount = balance;
        incomingAmount = 0;
        markersNotReceived.addAll(nodes.entrySet().parallelStream().filter(n -> n.getKey() != nodeId).map(Map.Entry::getKey).collect(Collectors.toSet()));
    }

    public void stopSnapshotRecording() {
        currentAmount = 0;
        incomingAmount = 0;
        markersNotReceived.clear();
    }

    public void incrementMoneyInTransfer(int recipientNodeId, int amount) {
        if (markersNotReceived.contains(recipientNodeId)) {
            incomingAmount += amount;
        }
    }

    public void stopRecordingNode(int nodeId) {
        markersNotReceived.remove(nodeId);
    }

    public boolean isRecording() {
        return markersNotReceived.size() != 0;
    }



    public int getCurrentAmount() {
        return currentAmount;
    }



    public void setCurrentAmount(int currentAmount) {
        this.currentAmount = currentAmount;
    }

    public int getIncomingAmount() {
        return incomingAmount;
    }

    public void setIncomingAmount(int incomingAmount) {
        this.incomingAmount = incomingAmount;
    }

    public int getSnapshotIdentifier() {
        return snapshotIdentifier;
    }

    public void setSnapshotIdentifier(int snapshotIdentifier) {
        this.snapshotIdentifier = snapshotIdentifier;
    }
}
