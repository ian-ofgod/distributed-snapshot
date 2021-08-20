package library;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class provides static methods to handle the storage of snapshots on disk.
 * It performs the creation of the destination folder and the snapshot saving.
 * */
class Storage {

    /**
     * Constant containing the folder name where snapshots will be saved
     * */
    private static final String FOLDER = "storage_folder";



    /**
     * Constant containing the extension of the files used to store snapshots on disk
     * */
    private static final String EXTENSION = ".data";



    /**
     * Method to create a folder for the snapshots to be saved.
     *
     * @param folderName*/
    private static void createFolder(String folderName) {
        try {
            Path path = Paths.get(FOLDER);
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }

            path = Paths.get(folderName);
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }
        } catch (IOException e) {
            System.err.println("Could not create folder");
        }
    }


    public static <StateType, MessageType> Snapshot<StateType, MessageType> readFile(int snapshotId) {
        Snapshot<StateType, MessageType> loaded_snapshot = new Snapshot<>(snapshotId);
        loaded_snapshot.messages = new HashMap<>();
        String folderName = buildFolderName(loaded_snapshot);
        try {
            File dir = new File(folderName);
            File[] directoryListing = dir.listFiles();
            if (directoryListing != null) {
                for (File child : directoryListing) { //for each entity
                    String filename = child.getName();
                    ArrayList<MessageType> empty_messages = new ArrayList<>();
                    if(filename.equals("state.ser")){ // I'm reading the state
                        FileInputStream fos = new FileInputStream(folderName + "state.ser");
                        ObjectInputStream oos = new ObjectInputStream(fos);
                        StateType state = (StateType) oos.readObject();
                        loaded_snapshot.state = state;
                    } else if (filename.equals("connectedNodes.ser")) { // I'm reading the list of nodes
                        FileInputStream fos = new FileInputStream(folderName + "connectedNodes.ser");
                        ObjectInputStream oos = new ObjectInputStream(fos);
                        ArrayList<Entity> connectedNodes  = (ArrayList<Entity>) oos.readObject();
                        loaded_snapshot.connectedNodes = connectedNodes;
                        //todo: valutare se funziona o se bisogna fare il parsing

                    } else { // I'm reading a message
                        String[] tokens = filename.split("_");
                        String ip = tokens[0];
                        int port = Integer.parseInt(tokens[1]);
                        int msg_idx = Integer.parseInt(tokens[tokens.length-1].substring(0,1));
                        Entity sender = new Entity(ip,port);

                        if (!loaded_snapshot.messages.containsKey(sender)){
                            loaded_snapshot.messages.put(sender,empty_messages);
                        }
                        FileInputStream fos = new FileInputStream(folderName + filename);
                        ObjectInputStream oos = new ObjectInputStream(fos);
                        MessageType message = (MessageType) oos.readObject();
                        loaded_snapshot.messages.get(sender).add(msg_idx-1,message);
                    }
                }
            } else {
                //TODO: controllare se serve gestire il caso in cui dir non e' davvero directory

                /*Checking dir.isDirectory() above would not be sufficient
                 to avoid race conditions with another process that deletes
                 directories.*/
            }


        } catch (IOException e) {
            System.err.println("Could not read file");
            e.printStackTrace();
        } catch (ClassNotFoundException e){
            System.err.println("Could not cast deserialized object to the expected type");
        }
        return loaded_snapshot;
    }

    /**
     * Method to save a snapshot portion on disk. It saves one folder per snapshot;
     * the source Entity (ip/port) is written in messages filename. It will be parsed.
     * @param <MessageType> this is the type that will be exchanged as a message between nodes
     * @param <StateType> this is the type that will be saved as the state of the application
     * @param runningSnapshots the list of snapshots running on the current node
     * @param snapshotId the id of the snapshot that the user want to save on disk
     * */
    public static <StateType, MessageType> void writeFile(ArrayList<Snapshot<StateType, MessageType>> runningSnapshots, int snapshotId) {

        Snapshot<StateType, MessageType> toSaveSnapshot = runningSnapshots.stream().filter(snap -> snap.snapshotId==snapshotId).findFirst().orElse(null);
        StateType state = toSaveSnapshot.state;
        HashMap<Entity, ArrayList<MessageType>> messageMap = toSaveSnapshot.messages;
        ArrayList<Entity> connectedNodes = toSaveSnapshot.connectedNodes;
        String folderName = buildFolderName(toSaveSnapshot);
        createFolder(folderName);

        try {
            FileOutputStream fos = new FileOutputStream(folderName+"state.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(state);
            Iterator entity = messageMap.keySet().iterator();
            while (entity.hasNext()) {
                Entity current_entity = (Entity) entity.next();
                String entity_identifier = current_entity.toString().replace(":","_");
                ArrayList<MessageType> current_messages = (ArrayList<MessageType>) messageMap.get(current_entity);
                for (int i =0; i < current_messages.size(); i++ ){
                    fos = new FileOutputStream(folderName+entity_identifier+"_message_"+(i+1)+".ser");
                    oos = new ObjectOutputStream(fos);
                    MessageType message = current_messages.get(i);
                    oos.writeObject(message);
                }
                //TODO: controllare se non rimuovere it genera effettivamente una ConcurrentModificationException, nel cui caso bisogna clonare prima
                //it.remove(); // avoids a ConcurrentModificationException
            }
            fos = new FileOutputStream(folderName+"connectedNodes.ser");
            oos = new ObjectOutputStream(fos);
            oos.writeObject(connectedNodes);

            oos.close();
            fos.close();
        } catch (IOException e) {
            System.err.println("Could not write file ");
        }
    }

    /**
     * Method to build the file name of the current snapshot before saving on disk.
     * @param <MessageType> this is the type that will be exchanged as a message between nodes
     * @param <StateType> this is the type that will be saved as the state of the application
     * @param snapshot the snapshot for which the name is built, will be saved on disk
     * */
    private static <StateType, MessageType> String buildFolderName(Snapshot<StateType, MessageType> snapshot) {
        return FOLDER + "/" + snapshot.snapshotId + "/" ;
    }


}
