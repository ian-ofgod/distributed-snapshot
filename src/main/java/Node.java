import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Objects;

public class Node extends RemoteImplementation {

    public Node(){}

    /*
                THIS ARE THE METHODS EXPOSED BY THE LIBRARY TO THE APPLICATION
     */

    //start the registry in the current node and bind our methods to the registry
    public void init(int rmi_port){
        try {
            RemoteImplementation obj = new RemoteImplementation();
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry = LocateRegistry.createRegistry(rmi_port);
            registry.bind("RemoteInterface", stub);
            System.err.println("Node ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    //add a new link, so look for his registry and save his reference
    //by having this method not exposed in the remoteInterface we disable the possibilty of adding connections
    //to a node by an external entity
    public void addConnection(String host, int rmi_port) {
        try {
            Registry registry = LocateRegistry.getRegistry(host, rmi_port);
            RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
            remoteInterfaces.put(new IpPort(host, rmi_port), remoteInterface);
        }
        catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    //send a message to a specific node using the saved remote object
    public <MessageType> void sendMessage(String host, int rmi_port, MessageType message){
        try {
            RemoteInterface remoteNode = remoteInterfaces.get(new IpPort(host, rmi_port));
            remoteNode.receiveMessage(message);
        }
        catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public <StateType> void updateState(StateType state){
        //TODO:
    }

    public void initiateSnapshot(){
        //TODO:
    }


    public static void main(String[] args) throws RemoteException {
      Node node1 = new Node();
      node1.init(1111);

      Node node2 = new Node();
      node2.init(1112);

      node1.addConnection("127.0.0.1",1112);
      node2.addConnection("127.0.0.1",1111);

      node1.sendMessage("127.0.0.1", 1112, new Message("Messaggio 1->2 che è stato processato da 2"));
      node2.sendMessage("127.0.0.1", 1111, new Message("Messaggio 2->1 che è stato processato da 1"));

    }
}

class IpPort{
    String ip;
    int port;

    IpPort(String ip, int port){
        this.ip=ip;
        this.port=port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IpPort ipPort = (IpPort) o;
        return port == ipPort.port && Objects.equals(ip, ipPort.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }
}