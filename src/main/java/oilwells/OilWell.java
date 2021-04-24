package oilwells;

import library.AppConnector;
import library.DistributedSnapshot;
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

/**
 * Main class of the application. It implements the AppConnector interface to receive calls from the DistributedSnapshot library.
 * It implements the public methods that the user can call from the command line interface. It uses a thread to periodically
 * send oil to a randomly chosen well.
 */
public class OilWell implements AppConnector<OilCargo> {
    /**
     * The amount of oil contained on the well
     */
    private int oilAmount = -1;

    /**
     * Lock object for oilAmount variable
     */
    private final Object oilAmountLock = new Object();

    /**
     *  The list of currently connected oil wells
     */
    private final ArrayList<ConnectionDetails> directConnections = new ArrayList<>();

    /**
     *  Lock object for directConnections variable
     */
    private final Object directConnectionsLock = new Object();

    /**
     * The library object used to interact with the library itself
     */
    private final DistributedSnapshot<Integer, OilCargo> distributedSnapshot = new DistributedSnapshot<>();

    /**
     * Logger used to print on the command line
     */
    private Logger logger;

    /**
     * Thread used to periodically send oil to other oil wells
     */
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * It is called to set the initial oil amount and set hostname and port inside the library
     */
    public void initialize(String hostname, int port, int oilAmount) {
        try {
            distributedSnapshot.init(hostname, port, this);
            this.oilAmount = oilAmount;
            distributedSnapshot.updateState(oilAmount);
            logger.info("Successfully initialized new node on " + hostname + ":" + port);
            startOilTransfers(2*1000, (int)(this.oilAmount*0.001), (int)(this.oilAmount*0.01));
        } catch (RemoteException | AlreadyBoundException e) {
            logger.warn("Cannot initialize new node");
        } catch (AlreadyInitialized e) {
            logger.info("You have already initialized your node!");
        }
    }

    /**
     * It is called to connect to another oil well
     */
    public void connect(String hostname, int port) {
        if (oilAmount != -1) {
            logger.info("Connecting to " + hostname + ":" + port);
            try {
                distributedSnapshot.addConnection(hostname, port);
                directConnections.add(new ConnectionDetails(hostname, port));
                logger.info("Successfully connected to " + hostname + ":" + port);
            } catch (RemoteException | NotBoundException | RemoteNodeAlreadyPresent e) {
                logger.warn("Cannot connect to " + hostname + ":" + port);
            } catch (NotInitialized notInitialized) {
                logger.info("You must first initialize your oil well!");
            }
        } else logger.info("You must first initialize your oil well!");
    }

    /**
     * It is called to disconnect from another oil well
     */
    public void disconnect(String hostname, int port) {
        if (oilAmount != -1) {
            logger.info("Disconnecting from " + hostname + ":" + port);
            try {
                distributedSnapshot.removeConnection(hostname, port);
                directConnections.remove(new ConnectionDetails(hostname, port));
                logger.info("Successfully disconnected from " + hostname + ":" + port);
            } catch (OperationForbidden | SnapshotInterruptException e) {
                logger.warn("You can't remove " + hostname + ":" + port);
            } catch (RemoteException e) {
                logger.warn("Cannot disconnect from " + hostname + ":" + port);
            } catch (NotInitialized notInitialized) {
                logger.info("You must first initialize your oil well!");
            }
        } else logger.info("You must first initialize your oil well!");
    }

    /**
     * It is called to initiate a snapshot on the network
     */
    public void snapshot() {
        if (oilAmount != -1) {
            logger.info("Starting snapshot");
            try {
                distributedSnapshot.initiateSnapshot();
                logger.info("Snapshot completed");
            } catch (RemoteException | DoubleMarkerException | UnexpectedMarkerReceived e) {
                logger.warn("Cannot complete snapshot");
            } catch (NotInitialized e) {
                logger.info("You must first initialize your oil well!");
            }
        } else logger.info("You must first initialize your oil well!");
    }

    /**
     * It is used to start the thread that periodically sends oil to another oil well
     */
    private void startOilTransfers(int frequency, int minAmount, int maxAmount) {
        logger.info("Starting automated oil transfers");
        executor.scheduleAtFixedRate(() -> {
            synchronized (directConnectionsLock) {
                if (directConnections.size() > 0) {
                    ConnectionDetails randomWell = directConnections.get((int) (Math.random() * (directConnections.size())));
                    try {
                        int amount = minAmount + (int) (Math.random() * ((maxAmount - minAmount) + 1));
                        synchronized (oilAmountLock) {
                            if (oilAmount - amount >= 0) {
                                distributedSnapshot.sendMessage(randomWell.getHostname(), randomWell.getPort(), new OilCargo(amount));
                                oilAmount -= amount;
                                distributedSnapshot.updateState(oilAmount);
                                logger.info("Sent " + amount + " oil to " + randomWell.getHostname() + ":" + randomWell.getPort() + ". New oilAmount = " + oilAmount);
                            } else {
                                logger.warn("You are running out of oil, cannot send oil to " + randomWell.getHostname() + ":" + randomWell.getPort());
                            }
                        }
                    } catch (RemoteNodeNotFound | RemoteException e) {
                        logger.warn("Error sending oil cargo. Removing connection to " + randomWell.getHostname() + ":" + randomWell.getPort());
                        directConnections.remove(randomWell);
                    } catch (NotBoundException | SnapshotInterruptException e) {
                        logger.warn("Error sending oil cargo");
                    } catch (NotInitialized notInitialized) {
                        logger.info("You must first initialize your oil well!");
                    }
                }
            }
        }, 0, frequency, TimeUnit.MILLISECONDS);
    }

    /**
     * It handles a new incoming message. It updates the oil amount contained on the oil well
     */
    @Override
    public void handleIncomingMessage(String senderIp, int senderPort, OilCargo message) {
        synchronized (oilAmountLock) {
            oilAmount += message.getOilAmount();
            distributedSnapshot.updateState(oilAmount);
            logger.info("Received " + message.getOilAmount() + " oil from " + senderIp + ":" + senderPort + ". New oilAmount = " + oilAmount);
        }
    }

    /**
     * It handles a new connection initiated from the other oil well. It updates the directConnections variable
     */
    @Override
    public void handleNewConnection(String newConnectionIp, int newConnectionPort) {
        synchronized (directConnectionsLock) {
            logger.info("Received connection attempt from " + newConnectionIp + ":" + newConnectionPort);
            directConnections.add(new ConnectionDetails(newConnectionIp, newConnectionPort));
            logger.info("Successfully connected to " + newConnectionIp + ":" + newConnectionPort);
        }
    }

    /**
     * It handles the removal of a connection initiated from the other oil well. It updates the directConnections variable
     */
    @Override
    public void handleRemoveConnection(String removeConnectionIp, int removeConnectionPort) {
        synchronized (directConnectionsLock) {
            logger.info("Received disconnect attempt from " + removeConnectionIp + ":" + removeConnectionPort);
            directConnections.remove(new ConnectionDetails(removeConnectionIp, removeConnectionPort));
            logger.info("Successfully disconnected from " + removeConnectionIp + ":" + removeConnectionPort);
        }
    }
}

/**
 * It's a pair of hostname and port
 * */
class ConnectionDetails {
    /**
     * The hostname of the oil well
     * */
    private final String hostname;

    /**
     * The port of the rmi registry of the oil well
     * */
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
