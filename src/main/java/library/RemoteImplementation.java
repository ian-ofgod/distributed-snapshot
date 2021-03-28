package library;

import java.rmi.RemoteException;
import java.util.ArrayList;

public class RemoteImplementation implements RemoteInterface {

    //store remote references to the linked nodes
    ArrayList<RemoteNode> remoteNodes = new ArrayList<>();

    //store the status of the node
    NodeStatus status = NodeStatus.IDLE;

    //this is the provided implementation of the class Observer
    private Observer observer;



    //useful to debug
    int id;

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

            //1)
            //loadStateFromDisk()

            //2)
            for (RemoteNode remoteNode : remoteNodes){
                try{
                    remoteNode.getRemoteInterface().receiveMessage(message);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            //3)
            status=NodeStatus.SNAPSHOT_RUNNING;

            //4)
            int numOfLinks=remoteNodes.size();
            for (RemoteNode remoteNode : remoteNodes){
                if(remoteNode.getMarkerIdReceived().contains(((MarkerMessage) message).marker_id)){
                    numOfLinks--;
                }
            }
            if(numOfLinks==0){
                //TODO: we have received a marker from all the channels
            }


            //for debug purposes
            System.out.println(id+") RECEIVED MARKER => initiator: "+((MarkerMessage) message).initiatorIp+":"+((MarkerMessage) message).initiatorPort);
        }
        else{
            if(status==NodeStatus.IDLE){ //no marker received
                //TODO: normal passing of the messages to the real application with the observer pattern
                //observer.notify(message); // NOT WORKING!
            }
            else if(status==NodeStatus.SNAPSHOT_RUNNING){ //snapshot running, marker received
                //TODO: we are running a snapshot and the current node has already received the marker so we have to save the current received message including the sender
                /*
                    1) get who sent the message
                    2) add the message and the sender to the local snapshot
                 */
            }

            //for debug purposes
            Message m= (Message) message;
            System.out.println(m.message);
        }
    }

    Observer getObserver(){
        return observer;
    }
    void setObserver(Observer o){
        this.observer=o;
    }

    public int getId() {
        return id;
    }

} 