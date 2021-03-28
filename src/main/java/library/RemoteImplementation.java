package library;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;


public class RemoteImplementation implements RemoteInterface {

    //store remote references to the linked nodes
    ArrayList<RemoteNode> remoteNodes = new ArrayList<>();

    //this is the provided implementation of the class Observer
    private Observer observer;

    ArrayList<Integer> runningSnapshotIds= new ArrayList<>();


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


            //for debug purposes
            System.out.println(id+") RECEIVED MARKER");
            if(!runningSnapshotIds.contains(((MarkerMessage) message).markerId)){
                runningSnapshotIds.add(((MarkerMessage) message).markerId);
            }


            //check if the received marker is the first one that i receive with that id
            boolean first_time=true;
            for (RemoteNode remoteNode : remoteNodes) {
                if (remoteNode.getMarkerIdsReceived().contains(((MarkerMessage) message).markerId)) {
                    //this is the case where there is already a running snapshot with that id so we just have to check if we have received a marker from all the nodes
                    first_time = false;
                    break;
                }
            }

            if(first_time){ //we have to send the marker to all the outgoing nodes
                System.out.println(id+") First time receiving a marker");

                //add the markerID in the markerIdsReceived to the node who sent the marker
                for (RemoteNode remoteNode : remoteNodes){
                    try{
                        if(remoteNode.ip_address.equals(RemoteServer.getClientHost())){
                            System.out.println(id+") Found the client who sent the marker");
                            remoteNode.getMarkerIdsReceived().add(((MarkerMessage) message).markerId);
                            break;
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

                //send the marker to all connected nodes
                for (RemoteNode remoteNode : remoteNodes) {
                    try {
                        if(!remoteNode.getMarkerIdsSent().contains(((MarkerMessage) message).markerId)) {
                            System.out.println(id+") Sending marker");
                            remoteNode.getMarkerIdsSent().add(((MarkerMessage) message).markerId);
                            remoteNode.getRemoteInterface().receiveMessage(message);
                        }else{
                            System.out.println(id+") Received a marker from a node where i have already sent the marker");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }else{ //we have to add the marker id to the remoteNode that has sent us the marker message
                System.out.println(id+") Already received marker");
                for (RemoteNode remoteNode : remoteNodes){
                    try{
                        if(remoteNode.ip_address.equals(RemoteServer.getClientHost())){
                            System.out.println(id+") Found the client who sent the marker");
                            if(remoteNode.getMarkerIdsReceived().contains(((MarkerMessage) message).markerId)){
                                System.out.println(id+") ERROR: received multiple marker (same id) for the same link");
                            }else{
                                System.out.println(id+") Added markerId for the remote node who called");
                                remoteNode.getMarkerIdsReceived().add(((MarkerMessage) message).markerId);
                            }
                            break;
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }

            //check if we have received marker from all the links
            int numOfLinks=remoteNodes.size();
            for (RemoteNode remoteNode : remoteNodes){
                if(remoteNode.getMarkerIdsReceived().contains(((MarkerMessage) message).markerId)){
                    numOfLinks--;
                }
            }
            if(numOfLinks==0){ //we have received a marker from all the channels
                System.out.println(id+") Received marker from all the channel");
                runningSnapshotIds.remove(Integer.valueOf(((MarkerMessage) message).markerId));
            }

        }
        else{
            //for debug purposes
            try {
                System.out.println(id+") Received a message from remoteNode: " + RemoteServer.getClientHost());
            }catch(Exception e){
                e.printStackTrace();
            }

            if(runningSnapshotIds.isEmpty()){ //no marker received
                //TODO: normal passing of the messages to the real application with the observer pattern
                observer.notify(message);
            }
            else if(!runningSnapshotIds.isEmpty()){ //snapshot running, marker received
                //TODO: we are running a snapshot and the current node has already received the marker so we have to save the current received message including the sender
                /*
                    1) get who sent the message
                    2) add the message and the sender to the local snapshot
                 */
                observer.notify(message);
            }

        }
    }

    @Override
    public void addMeBack(String ip_address, int port){
        //TODO: NOT WORKING
        try {
            Registry registry = LocateRegistry.getRegistry(ip_address, port);
            RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
            remoteNodes.add(new RemoteNode(ip_address,port,remoteInterface));
        }
        catch (Exception e) {
            e.printStackTrace();
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