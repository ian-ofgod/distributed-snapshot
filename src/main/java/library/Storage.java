package library;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * This class provides static methods to handle the storage of snapshots on disk.
 * It performs the creation of the destination folder and the snapshot saving.
 * */
class Storage {

    /**
     * Constant containing the folder name where snapshots will be saved
     * */
    private static final String FOLDER = "storage_folder";

    private static int COUNTER = 0;

    /**
     * Method to create a folder for the snapshots to be saved
     * @param folderName the name of the folder to create
     * */
    private  static void createFolder(String folderName, String currentHostname, int currentPort) {
        try {
            Path path = Paths.get(FOLDER);
            if (!Files.exists(path)) {
                try {
                    Files.createDirectory(path);
                } catch (FileAlreadyExistsException ignored) {}
            }

            path = Paths.get(FOLDER + "/" + currentHostname + "_" + currentPort + "/");
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }

            path = Paths.get(folderName);
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }
        } catch (IOException e) {
            System.err.println("Could not create folder");
            e.printStackTrace();
        }
    }

    /**
     * Retrieve the id of the last snapshot available to be restored
     * @param currentHostname the hostname of the node requesting this information
     * @param currentPort the port of the node requesting this information
     * @return the id of the last snapshot
     */
    public static int getLastSnapshotId(String currentHostname, int currentPort) {

        File dir = new File(FOLDER + "/" + currentHostname + "_" + currentPort + "/");
        File[] directoryListing = dir.listFiles();
        HashMap<Integer,Integer> allSnaps = new HashMap<>();
        if (directoryListing != null) {
            for (File child : directoryListing) { //for each entity
                String filename = child.getName();
                String[] tokens = filename.split("_");
                allSnaps.put(Integer.parseInt(tokens[0]),Integer.parseInt(tokens[1]));
            }

        }
        Integer max_key = -1, max_snapshotId=-1;

        for (HashMap.Entry<Integer, Integer> entry : allSnaps.entrySet()) {
            Integer key = entry.getKey();
            Integer snapshotId = entry.getValue();
            if(key>max_key) {
                max_key = key;
                max_snapshotId = snapshotId;
            }
        }

        return max_snapshotId;
    }

    public synchronized static <StateType, MessageType> Snapshot<StateType, MessageType> readFile(int snapshotId, String currentHostname, int currentPort) throws IOException, ClassNotFoundException {
        Snapshot<StateType, MessageType> loaded_snapshot = new Snapshot<>(snapshotId);
        loaded_snapshot.messages = new ArrayList<>();
        File dir = new File(FOLDER + "/" + currentHostname + "_" + currentPort + "/");
        File[] all_snaps = dir.listFiles();
        assert all_snaps != null;
        for (File folder : all_snaps){
            if(folder.getAbsolutePath().contains(String.valueOf(snapshotId))){
                String folderName = folder.getAbsolutePath() + "/";
                try {
                    File dir2 = new File(folderName);
                    File[] directoryListing = dir2.listFiles();
                    if (directoryListing != null) {
                        for (File child : directoryListing) { //for each entity
                            String filename = child.getName();
                            if(filename.equals("state.ser")){ // I'm reading the state
                                FileInputStream fos = new FileInputStream(folderName + "state.ser");
                                ObjectInputStream oos = new ObjectInputStream(fos);
                                loaded_snapshot.state = (StateType) oos.readObject();
                                oos.close();
                            } else if (filename.equals("connectedNodes.ser")) { // I'm reading the list of nodes
                                FileInputStream fos = new FileInputStream(folderName + "connectedNodes.ser");
                                ObjectInputStream oos = new ObjectInputStream(fos);
                                loaded_snapshot.connectedNodes = (ArrayList<Entity>) oos.readObject();
                                oos.close();
                            } else { // I'm reading a message
                                String[] tokens = filename.split("_");
                                String ip = tokens[0];
                                int port = Integer.parseInt(tokens[1]);
                                Entity sender = new Entity(ip,port);
                                FileInputStream fos = new FileInputStream(folderName + filename);
                                ObjectInputStream oos = new ObjectInputStream(fos);
                                MessageType message = (MessageType) oos.readObject();
                                oos.close();
                                loaded_snapshot.messages.add(new Envelope<>(sender, message));
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Could not read file");
                    throw e;
                } catch (ClassNotFoundException e){
                    System.err.println("Could not cast deserialized object to the expected type");
                    throw e;
                }
            }
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
    public synchronized static <StateType, MessageType> void writeFile(ArrayList<Snapshot<StateType, MessageType>> runningSnapshots, int snapshotId, String currentHostname, int currentPort) throws IOException {
        COUNTER++;
        Snapshot<StateType, MessageType> toSaveSnapshot = runningSnapshots.stream().filter(snap -> snap.snapshotId==snapshotId).findFirst().orElse(null);
        assert toSaveSnapshot != null;
        StateType state = toSaveSnapshot.state;
        ArrayList<Envelope<MessageType>> envelopes = toSaveSnapshot.messages;
        ArrayList<Entity> connectedNodes = toSaveSnapshot.connectedNodes;
        String folderName = buildFolderName(toSaveSnapshot,currentHostname,currentPort);
        createFolder(folderName, currentHostname, currentPort);

        try {
            System.out.println("####################################################");
            System.out.println("["+currentHostname+":"+currentPort+"] oilAmount inside of the snapshot: "+state.toString());
            System.out.println("####################################################");

            FileOutputStream fos = new FileOutputStream(folderName+"state.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(state);
            oos.close();

            int i=0; // global id for messages
            for (Envelope<MessageType> envelope : envelopes) {
                String entity_identifier = envelope.sender.toString().replace(":", "_");
                fos = new FileOutputStream(folderName + entity_identifier + "_message_" + (++i) + ".ser");
                oos = new ObjectOutputStream(fos);
                oos.writeObject(envelope.message);
                oos.close();
            }
            fos = new FileOutputStream(folderName+"connectedNodes.ser");
            oos = new ObjectOutputStream(fos);
            oos.writeObject(connectedNodes);
            oos.close();
        } catch (IOException e) {
            System.err.println("Could not write file ");
           throw e;
        }
    }



    /**
     * Method to build the file name of the current snapshot before saving on disk.
     * @param <MessageType> this is the type that will be exchanged as a message between nodes
     * @param <StateType> this is the type that will be saved as the state of the application
     * @param snapshot the snapshot for which the name is built, will be saved on disk
     * */
    private static <StateType, MessageType> String buildFolderName(Snapshot<StateType, MessageType> snapshot, String currentHostname, int currentPort) {
        return FOLDER + "/" + currentHostname + "_" + currentPort + "/" + COUNTER +
                "_"+ snapshot.snapshotId + "/" ;
    }


    /**
     * Method to be called to clean the storage folder
     * @throws IOException thrown if something went wrong (for example files still open by other processes)
     */
    public synchronized static void cleanStorageFolder() throws IOException {
        if (new File(FOLDER).isDirectory()) {
            try {
                FileUtils.deleteDirectory(new File(FOLDER));
            } catch (IOException e) {
                System.out.println("Unable to delete folder");
                throw e;
            }
        }
    }
    


}
