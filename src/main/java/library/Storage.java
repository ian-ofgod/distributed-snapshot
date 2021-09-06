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
     * Constant containing the extension of the files used to store snapshots on disk
     * */
    private static final String EXTENSION = ".data";



    /**
     * Method to create a folder for the snapshots to be saved.
     *
     * @param folderName*/
    private  static void createFolder(String folderName, String currentIp, int currentPort) {
        try {
            Path path = Paths.get(FOLDER);
            if (!Files.exists(path)) {
                try {
                    Files.createDirectory(path);
                } catch (FileAlreadyExistsException ignored) {}
            }

            path = Paths.get(FOLDER + "/" + currentIp + "_" + currentPort + "/");
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

    public static int getLastSnapshotId(String currentIp, int currentPort) {

        File dir = new File(FOLDER + "/" + currentIp + "_" + currentPort + "/");
        File[] directoryListing = dir.listFiles();
        HashMap<Integer,Integer> allSnaps = new HashMap<>();
        if (directoryListing != null) {
            for (File child : directoryListing) { //for each entity
                String filename = child.getName();
                String[] tokens = filename.split("_");
                allSnaps.put(Integer.parseInt(tokens[0]),Integer.parseInt(tokens[1]));
            }

        }
        return allSnaps.get(Collections.max(allSnaps.keySet()));
    }

    public synchronized static <StateType, MessageType> Snapshot<StateType, MessageType> readFile(int snapshotId, String currentIp, int currentPort) throws IOException, ClassNotFoundException {
        Snapshot<StateType, MessageType> loaded_snapshot = new Snapshot<>(snapshotId);
        loaded_snapshot.messages = new ArrayList<>();
        File dir = new File(FOLDER + "/" + currentIp + "_" + currentPort + "/");
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
                                int msg_idx = Integer.parseInt(tokens[tokens.length-1].substring(0,1));
                                Entity sender = new Entity(ip,port);
                                FileInputStream fos = new FileInputStream(folderName + filename);
                                ObjectInputStream oos = new ObjectInputStream(fos);
                                MessageType message = (MessageType) oos.readObject();
                                oos.close();
                                loaded_snapshot.messages.add(new Envelope<>(sender, message));
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
    public synchronized static <StateType, MessageType> void writeFile(ArrayList<Snapshot<StateType, MessageType>> runningSnapshots, int snapshotId, String currentIp, int currentPort) throws IOException {
        COUNTER++;
        Snapshot<StateType, MessageType> toSaveSnapshot = runningSnapshots.stream().filter(snap -> snap.snapshotId==snapshotId).findFirst().orElse(null);
        assert toSaveSnapshot != null;
        StateType state = toSaveSnapshot.state;
        ArrayList<Envelope<MessageType>> envelopes = toSaveSnapshot.messages;
        ArrayList<Entity> connectedNodes = toSaveSnapshot.connectedNodes;
        String folderName = buildFolderName(toSaveSnapshot,currentIp,currentPort);
        createFolder(folderName, currentIp, currentPort);

        try {
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

                //TODO: controllare se non rimuovere it genera effettivamente una ConcurrentModificationException, nel cui caso bisogna clonare prima
                //it.remove(); // avoids a ConcurrentModificationException
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
    private static <StateType, MessageType> String buildFolderName(Snapshot<StateType, MessageType> snapshot, String currentIp, int currentPort) {
        return FOLDER + "/" + currentIp + "_" + currentPort + "/" + COUNTER +
                "_"+ snapshot.snapshotId + "/" ;
    }


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
