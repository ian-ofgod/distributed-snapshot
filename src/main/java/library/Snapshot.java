package library;

public class Snapshot {
    // TODO: here we assume that all snapshots have CURRENT and INCOMING fields only
    int currentAmount;

   int incomingAmount;

    public Snapshot(int currentAmount, int incomingAmount) {
        this.currentAmount = currentAmount;
        this.incomingAmount = incomingAmount;
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

}
