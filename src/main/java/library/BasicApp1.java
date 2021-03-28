package library;

public class BasicApp1 implements AppConnector {
    @Override
    public void handleIncomingMessage(String senderIp, int senderPort, Object o) {
        System.out.println("BASIC APP 1: HANDLING THE MESSAGGEEEEOIHDAUHEF");
    }
}
