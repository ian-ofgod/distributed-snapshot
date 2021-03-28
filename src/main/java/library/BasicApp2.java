package library;

public class BasicApp2 implements AppConnector {
    @Override
    public void handleIncomingMessage(String senderIp, int senderPort, Object o) {
        System.out.println("BASIC APP 2 HANDLING THE MESSAGE");
    }
}
