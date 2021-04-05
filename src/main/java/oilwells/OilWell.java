package oilwells;

import library.AppConnector;
import library.Node;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OilWell implements AppConnector {
    private String hostname;
    private int port;
    private int oilAmount;

    private ArrayList<ConnectionDetails> directConnections = new ArrayList<>();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public void initialize(String hostname, int port, int oilAmount) {
        this.hostname = hostname;
        this.port = port;
        this.oilAmount = oilAmount;
        //TODO: Check if new Node has failed
        Node.init(hostname, port, this);
        System.out.println("Successfully initialized new node on " + hostname + ":" + port);
        startOilTransfers(500, (int)(this.oilAmount*0.001), (int)(this.oilAmount*0.01));
    }

    public void connect(String hostname, int port) {
        //TODO: check if node.addConnection has failed
        System.out.println("Connecting to " + hostname + ":" + port);
        Node.addConnection(hostname, port);
        directConnections.add(new ConnectionDetails(hostname, port));
        System.out.println("Successfully connected to " + hostname + ":" + port);
    }

    public void snapshot() {
        Node.initiateSnapshot();
    }

    private void startOilTransfers(int frequency, int minAmount, int maxAmount) {
        System.out.println("Starting automated oil transfers");
        executor.scheduleAtFixedRate(() -> {
            if (directConnections.size() > 0) {
                try {
                    ConnectionDetails randomWell = directConnections.get((int)(Math.random() * (directConnections.size())));
                    int amount = minAmount + (int) (Math.random() * ((maxAmount - minAmount) + 1));
                    if (oilAmount - amount >= 0) {
                        oilAmount -= amount;
                        System.out.println("Sending " + amount + " oil to " + randomWell.getHostname() + ":" + randomWell.getPort() + ". New oilAmount = " + oilAmount);
                        //TODO: add/change exception handling
                        Node.sendMessage(randomWell.getHostname(), randomWell.getPort(), new OilCargo(amount));
                    } else {
                        System.out.println("You are running out of oil, cannot send oil to " + randomWell.getHostname() + ":" + randomWell.getPort());
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, frequency, TimeUnit.MILLISECONDS);
    }

    @Override
    public void handleIncomingMessage(String senderIp, int senderPort, Object o) {
        OilCargo message = (OilCargo) o;
        oilAmount += message.getOilAmount();
        System.out.println("Received " + message.getOilAmount() + " oil from " + senderIp + ":" + senderPort + ". New oilAmount = " + oilAmount);
    }

    @Override
    public void handleNewConnection(String newConnectionIp, int newConnectionPort) {
        directConnections.add(new ConnectionDetails(newConnectionIp, newConnectionPort));
        System.out.println("Successfully connected to " + newConnectionIp + ":" + newConnectionPort);
    }

    @Override
    public void handleRemoveConnection(String removeConnectionIp, int removeConnectionPort) {

    }
}

class ConnectionDetails {
    private String hostname;
    private int port;

    public ConnectionDetails(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }
}
