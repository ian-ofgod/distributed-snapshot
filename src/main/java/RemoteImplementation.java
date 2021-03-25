import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RemoteImplementation implements RemoteInterface {

    @Override
    public void printMsg() {
        System.out.println("Computed by the remote node");
    }

    @Override
    public void receiveMessage(Message msg) throws RemoteException {
        System.out.println(msg.message);
    }


} 