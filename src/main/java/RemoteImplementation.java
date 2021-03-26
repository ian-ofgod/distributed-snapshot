import java.rmi.RemoteException;
import java.util.HashMap;

public class RemoteImplementation implements RemoteInterface {

    //store remote references to the linked nodes
    HashMap<IpPort,RemoteInterface> remoteInterfaces = new HashMap<>();

    //status of this remoteNode 0==snapshot not started / 1==snapshot started
    int status; //TODO change to enum

    @Override
    public <MessageType> void receiveMessage(MessageType message) throws RemoteException {
        if(message.getClass()==MarkerMessage.class){
            //TODO: handle the receive of the marker
            /*
                1) pickup the current saved state of this node from disk and lock it
                2) send to all outgoing channels the marker message
                3) update your status to snapshot started
                4) check if you have received the marker from all the incoming channels
                    -> if so, send your local snapshot to the host specified in the marker?
             */

            //for debug purposes
            status=1;
        } else{
            if(status==0){ //no marker received
                //TODO: normal passing of the messages to the real application with the observer pattern
            }
            else if(status==1){ //snapshot running, marker received
                //TODO: we are running a snapshot and the current node has already received the marker so we have to
                //TODO: save the current received message including the sender
            }

            //for debug purposes
            Message m= (Message) message;
            System.out.println(m.message);
        }
    }

} 