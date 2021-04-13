package oilwells;

import library.AppConnector;
import library.Node;
import library.exceptions.*;
import org.apache.logging.log4j.Logger;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OilWell implements AppConnector {
    private String hostname;
    private int port;
    private int oilAmount;
    private final Object oilAmountLock = new Object();
    private final ArrayList<ConnectionDetails> directConnections = new ArrayList<>();
    private final Object directConnectionsLock = new Object();

    private final Node<Integer, OilCargo> node = new Node<>();

    private Logger logger;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void initialize(String hostname, int port, int oilAmount) {
        this.hostname = hostname;
        this.port = port;
        this.oilAmount = oilAmount;
        try {
            node.init(hostname, port, this);
            logger.info("Successfully initialized new node on " + hostname + ":" + port);
            startOilTransfers(2*1000, (int)(this.oilAmount*0.001), (int)(this.oilAmount*0.01));
        } catch (RemoteException | AlreadyBoundException e) {
            logger.warn("Cannot initialize new node");
        }
    }

    public void connect(String hostname, int port) {
        logger.info("Connecting to " + hostname + ":" + port);
        try {
            node.addConnection(hostname, port);
            directConnections.add(new ConnectionDetails(hostname, port));
            logger.info("Successfully connected to " + hostname + ":" + port);
        } catch (RemoteException | NotBoundException e) {
            logger.warn("Cannot connect to " + hostname + ":" + port);
        }
    }

    public void disconnect(String hostname, int port) {
        logger.info("Disconnecting from " + hostname + ":" + port);
        try {
            node.removeConnection(hostname, port);
            directConnections.remove(new ConnectionDetails(hostname, port));
            logger.info("Successfully disconnected from " + hostname + ":" + port);
        } catch (OperationForbidden | SnapshotInterruptException e){
            logger.warn("You can't remove " + hostname + ":" + port);
        } catch (RemoteException e) {
            logger.warn("Cannot disconnect from " + hostname + ":" + port);
        }
    }

    public void snapshot() {
        logger.info("Starting snapshot");
        try {
            node.initiateSnapshot();
            logger.info("Snapshot completed");
        } catch (RemoteException | DoubleMarkerException e) {
            logger.warn("Cannot complete snapshot");
        }
    }

    private void startOilTransfers(int frequency, int minAmount, int maxAmount) {
        logger.info("Starting automated oil transfers");
        executor.scheduleAtFixedRate(() -> {
            synchronized (directConnectionsLock) {
                if (directConnections.size() > 0) {
                    try {
                        ConnectionDetails randomWell = directConnections.get((int) (Math.random() * (directConnections.size())));
                        int amount = minAmount + (int) (Math.random() * ((maxAmount - minAmount) + 1));
                        synchronized (oilAmountLock) {
                            if (oilAmount - amount >= 0) {
                                logger.info("Sending " + amount + " oil to " + randomWell.getHostname() + ":" + randomWell.getPort() + ". New oilAmount = " + oilAmount);
                                node.sendMessage(randomWell.getHostname(), randomWell.getPort(), new OilCargo(amount));
                                oilAmount -= amount;
                            } else {
                                logger.warn("You are running out of oil, cannot send oil to " + randomWell.getHostname() + ":" + randomWell.getPort());
                            }
                        }
                    } catch (RemoteNodeNotFound | RemoteException e) {
                        logger.warn("Error sending oil cargo");
                    }
                }
            }
        }, 0, frequency, TimeUnit.MILLISECONDS);
    }

    @Override
    public void handleIncomingMessage(String senderIp, int senderPort, Object o) {
        synchronized (oilAmountLock) {
            OilCargo message = (OilCargo) o;
            oilAmount += message.getOilAmount();
            logger.info("Received " + message.getOilAmount() + " oil from " + senderIp + ":" + senderPort + ". New oilAmount = " + oilAmount);
        }
    }

    @Override
    public void handleNewConnection(String newConnectionIp, int newConnectionPort) {
        synchronized (directConnectionsLock) {
            logger.info("Received connection attempt from " + newConnectionIp + ":" + newConnectionPort);
            directConnections.add(new ConnectionDetails(newConnectionIp, newConnectionPort));
            logger.info("Successfully connected to " + newConnectionIp + ":" + newConnectionPort);
        }
    }

    @Override
    public void handleRemoveConnection(String removeConnectionIp, int removeConnectionPort) {
        synchronized (directConnectionsLock) {
            logger.info("Received disconnect attempt from " + removeConnectionIp + ":" + removeConnectionPort);
            directConnections.remove(new ConnectionDetails(removeConnectionIp, removeConnectionPort));
            logger.info("Successfully disconnected from " + removeConnectionIp + ":" + removeConnectionPort);
        }
    }
}

class ConnectionDetails {
    private final String hostname;
    private final int port;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConnectionDetails that = (ConnectionDetails) o;

        if (port != that.port) return false;
        return Objects.equals(hostname, that.hostname);
    }

    @Override
    public int hashCode() {
        int result = hostname != null ? hostname.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }
}
