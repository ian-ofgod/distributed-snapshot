import java.io.Serializable;

public class MarkerMessage implements Serializable {
    String initiatorIp;
    int initiatorPort;
    int marker_id;

    public MarkerMessage(String initiatorIp, int initiatorPort) {
        this.initiatorIp = initiatorIp;
        this.initiatorPort = initiatorPort;
    }
}
