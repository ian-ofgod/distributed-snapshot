package oilwells;

import library.Node;
import library.AppConnector;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OilWell implements AppConnector {

    private String hostname;
    private int port;
    private int oilAmount;

    private ArrayList<ConnectionDetails> directConnections = new ArrayList<>();

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    //TODO: remove and make Node static (?)
    private Node node;

    public void initialize(String hostname, int port, int oilAmount) {
        this.hostname = hostname;
        this.port = port;
        this.oilAmount = oilAmount;
        //TODO: Check if new Node has failed
        node = new Node(this,hostname,port);
    }

    public void connect(String hostname, int port) {
        //TODO: check if node.addConnection has failed
        node.addConnection(hostname, port);
        directConnections.add(new ConnectionDetails(hostname, port));
    }

    public void snapshot() {
        node.initiateSnapshot();
    }

    public static void main(String[] args) {
        System.out.println("Welcome to the oil-wells system!");
        System.out.println("Type in: action name, hostname, port, (oilAmount)");
        System.out.println("Example: initialize, localhost, 10000, 1000");
        System.out.println("Example: connect, localhost, 10001");
        System.out.println("Example: snapshot");
        Parser.parseInput(OilWell.class.getName());
    }


    private void startOilTransfers(int frequency, int minAmount, int maxAmount) {
        executor.scheduleAtFixedRate((Runnable) () -> {
            ConnectionDetails randomWell = directConnections.get((int)(Math.random() * (directConnections.size() + 1)));
            node.sendMessage(randomWell.getHostname(), randomWell.getPort(), new OilCargo(minAmount + (int)(Math.random() * ((maxAmount - minAmount) + 1))));
        }, 0, frequency, TimeUnit.MILLISECONDS);
    }

    @Override
    public void handleIncomingMessage(String senderIp, int senderPort, Object o) {

    }

    @Override
    public void handleNewConnection(String newConnectionIp, int newConnectionPort) {

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
