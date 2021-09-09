package oilwells;

import library.AppConnector;
import library.DistributedSnapshot;
import library.exceptions.*;
import library.Entity;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
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
public class OilWell implements AppConnector<OilCargo, Integer> {
    /**
     * The amount of oil contained on the well
     */
    private int oilAmount = -1;

    /**
     * Lock-object for oilAmount variable
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
     * @param hostname the hostname of this node
     * @param port the port of this node
     * @param oilAmount the initial oil amount
     * @throws StateUpdateException thrown if an error occur during the update of the state
     */
    public void initialize(String hostname, int port, int oilAmount) throws StateUpdateException {
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
        } catch (RestoreInProgress e) {
            logger.info(e.getMessage());
        } catch (NotInitialized ignored) {}
    }

    /**
     * It is called to join an existing network of nodes
     * @param hostname the hostname of the gateway used to join the network
     * @param port the port of the gateway used to join the network
     */
    public void join(String hostname, int port) {
        if (oilAmount != -1) {
            synchronized (directConnectionsLock) {
                if (directConnections.size() == 0) {
                    logger.info("Connecting to " + hostname + ":" + port + " to join a network");
                    try {
                        ArrayList<Entity> nodes = distributedSnapshot.joinNetwork(hostname, port);
                        StringBuilder connectedNodes = new StringBuilder();
                        for (Entity entry : nodes) {
                            directConnections.add(new ConnectionDetails(entry.getHostname(), entry.getPort()));
                            connectedNodes.append(", ").append(entry.getHostname()).append(":").append(entry.getPort());
                        }
                        logger.info("Successfully connected to: " + connectedNodes.substring(2));
                    } catch (RemoteException | NotBoundException e) {
                        logger.warn("Error joining network through " + hostname + ":" + port);
                    } catch (NotInitialized notInitialized) {
                        logger.info("You must first initialize your oil well!");
                    } catch (OperationForbidden operationForbidden) {
                        logger.info(operationForbidden.getMessage());
                    }
                } else logger.info("You are already connected to a network");
            }
        } else logger.info("You must first initialize your oil well!");
    }

    /**
     * It is called to disconnect from an existing network
     */
    public void disconnect() {
        if (oilAmount != -1) {
            logger.info("Disconnecting from the network");
            synchronized (directConnectionsLock) {
                try {
                    distributedSnapshot.disconnect();
                    directConnections.clear();
                } catch (OperationForbidden | SnapshotInterruptException e) {
                    logger.warn("Cannot disconnect from the network");
                } catch (NotInitialized notInitialized) {
                    logger.info("You must first initialize your oil well!");
                } catch (RestoreInProgress e) {
                    logger.info(e.getMessage());
                }
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
            } catch (RestoreInProgress | IOException e) {
                logger.info(e.getMessage());
            }
        } else logger.info("You must first initialize your oil well!");
    }

    /**
     * It is called to initiate a restore of a previously taken snapshot
     */
    public void restore() {
        if (oilAmount != -1) {
            try {
                distributedSnapshot.restoreLastSnapshot();
                logger.info("################################################");
                logger.info("Successfully restored last snapshot. New oilAmount = " + oilAmount);
                logger.info("################################################");
            } catch (IOException | ClassNotFoundException | RestoreAlreadyInProgress | NotBoundException | RestoreInProgress | RestoreNotPossible e) {
                logger.warn("Cannot restore snapshot");
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
                        logger.warn("Error sending oil cargo. Disconnecting from " + randomWell.getHostname() + ":" + randomWell.getPort());
                        try {
                            distributedSnapshot.removeNode(randomWell.getHostname(), randomWell.getPort());
                            directConnections.remove(randomWell);
                        } catch (RemoteException | RestoreInProgress | SnapshotInterruptException ex) {
                            logger.warn("Error removing node " + randomWell.getHostname() + ":" + randomWell.getPort());
                        } catch (NotInitialized ignored) {}
                    } catch (NotBoundException | SnapshotInterruptException e) {
                        logger.warn("Error sending oil cargo");
                    } catch (NotInitialized notInitialized) {
                        logger.info("You must first initialize your oil well!");
                    } catch (StateUpdateException e) {
                        logger.info("Error in updating state");
                    } catch (RestoreInProgress restoreInProgress) {
                        logger.info("Cannot send oil while a restore is in progress");
                    } catch (OperationForbidden operationForbidden) {
                        logger.info(operationForbidden.getMessage());
                    }
                }
            }
        }, 0, frequency, TimeUnit.MILLISECONDS);
    }

    @Override
    public void handleRestoredState(Integer state) {
        synchronized (oilAmountLock) {
            oilAmount = state;
        }
        logger.info("################################################");
        logger.info("Restored snapshot. New oilAmount = " + oilAmount);
        logger.info("################################################");
    }

    @Override
    public void handleRestoredConnections(ArrayList<Entity> connections) {
        synchronized (directConnectionsLock) {
            directConnections.clear();
            for (Entity entry : connections) {
                directConnections.add(new ConnectionDetails(entry.getHostname(), entry.getPort()));
            }
        }
    }

    /**
     * It handles a new incoming message. It updates the oil amount contained on the oil well
     */
    @Override
    public void handleIncomingMessage(String senderHostname, int senderPort, OilCargo message) {
        synchronized (oilAmountLock) {
            oilAmount += message.getOilAmount();
            try {
                distributedSnapshot.updateState(oilAmount);
                logger.info("Received " + message.getOilAmount() + " oil from " + senderHostname + ":" + senderPort + ". New oilAmount = " + oilAmount);
            } catch (StateUpdateException | RestoreInProgress e) {
                logger.warn("Received " + message.getOilAmount() + " oil from " + senderHostname + ":" + senderPort + ". Error updating state");
            } catch (NotInitialized ignored) {}
        }
    }

    /**
     * It handles a new connection initiated from the other oil well. It updates the directConnections variable
     */
    @Override
    public void handleNewConnection(String newConnectionHostname, int newConnectionPort) {
        synchronized (directConnectionsLock) {
            logger.info("Received connection attempt from " + newConnectionHostname + ":" + newConnectionPort);
            directConnections.add(new ConnectionDetails(newConnectionHostname, newConnectionPort));
            logger.info("Successfully connected to " + newConnectionHostname + ":" + newConnectionPort);
        }
    }

    /**
     * It handles the removal of a connection initiated from the other oil well. It updates the directConnections variable
     */
    @Override
    public void handleRemoveConnection(String removeConnectionHostname, int removeConnectionPort) {
        synchronized (directConnectionsLock) {
            logger.info("Received disconnect attempt from " + removeConnectionHostname + ":" + removeConnectionPort);
            directConnections.remove(new ConnectionDetails(removeConnectionHostname, removeConnectionPort));
            logger.info("Successfully disconnected from " + removeConnectionHostname + ":" + removeConnectionPort);
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
