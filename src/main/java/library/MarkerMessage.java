package library;

import java.io.Serializable;

public class MarkerMessage implements Serializable {
    String initiatorIp;
    int initiatorPort;
    int markerId;

    public MarkerMessage(String initiatorIp, int initiatorPort, int markerId) {
        this.initiatorIp = initiatorIp;
        this.initiatorPort = initiatorPort;
        this.markerId=markerId;
    }
}
