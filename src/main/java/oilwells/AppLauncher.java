package oilwells;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class AppLauncher {
    private static OilWell oilWell = new OilWell();
    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) {
        oilWell.setLogger(logger);
        logger.info("Welcome to the oil-wells system!");
        logger.info("Type in: action name, hostname, port, (oilAmount)");
        logger.info("Example: initialize, localhost, 10000, 1000");
        logger.info("Example: connect, localhost, 10001");
        logger.info("Example: snapshot");
        Parser.parseInput(OilWell.class.getName(), oilWell);
    }
}
