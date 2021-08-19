package oilwells;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The startup class that it's called on startup
 * */
public final class AppLauncher {
    /**
     * The oil well instance
     * */
    private static final OilWell oilWell = new OilWell();

    /**
     * The logger instance used to print on the command line
     * */
    private static final Logger logger = LogManager.getLogger();

    /**
     * It sets the logger and calls the parser to handle command line inputs
     * */
    public static void main(String[] args) {
        oilWell.setLogger(logger);
        logger.info("Welcome to the oil-wells system!");
        logger.info("Type in: action name, hostname, port, (oilAmount)");
        logger.info("Example: initialize, localhost, 10000, 1000");
        logger.info("Example: join, localhost, 10001");
        logger.info("Example: snapshot");
        logger.info("Example: disconnect");
        while (true) {
            try {
                Parser.parseInput(OilWell.class.getName(), oilWell);
            } catch (IllegalStateException e) {
                logger.warn(e.getMessage());
            }
        }
    }
}
