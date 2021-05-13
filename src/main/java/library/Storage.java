package library;

import org.apache.logging.log4j.message.Message;

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
        } catch (Exception e) {
            //TODO: remove SystemPrintln
            //TODO: evitare generic exceptions
            System.err.println("Could not create folder");
        }
    }


    public static <StateType, MessageType> Snapshot<StateType, MessageType> readFile(String folder, int snapshotId) {

        Snapshot<StateType, MessageType> loaded_snapshot = new Snapshot<>(snapshotId);

        return loaded_snapshot;
    }

    /**
     * Method to save a snapshot portion on disk.
     * @param <MessageType> this is the type that will be exchanged as a message between nodes
     * @param <StateType> this is the type that will be saved as the state of the application
     * @param runningSnapshots the list of snapshots running on the current node
     * @param snapshotId the id of the snapshot that the user want to save on disk
     * */
    //TODO: Testare!
    public static <StateType, MessageType> void writeFile(ArrayList<Snapshot<StateType, MessageType>> runningSnapshots, int snapshotId) {

        Snapshot<StateType, MessageType> toSaveSnapshot = runningSnapshots.stream().filter(snap -> snap.snapshotId==snapshotId).findFirst().orElse(null);
        StateType state = toSaveSnapshot.state;
        HashMap<Entity, ArrayList<MessageType>> messageMap = toSaveSnapshot.messages;

        // ONE FOLDER PER SNAPSHOT - THEN SOURCE ENTITY IS WRITTEN IN MESSAGES FILENAME
        String folderName = buildFolderName(toSaveSnapshot);
        createFolder(folderName);

        try {
            FileOutputStream fos = new FileOutputStream(folderName+"state.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            // write object to file
            oos.writeObject(state);
            System.out.println("State serialized and saved to file.");


            Iterator it = messageMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                Entity current_entity = (Entity) pair.getKey();
                String entity_identifier = current_entity.toString().replace(":","_");
                ArrayList<MessageType> current_messages = (ArrayList<MessageType>) pair.getValue();

                for (int i =0; i < current_messages.size(); i++ ){
                    fos = new FileOutputStream(folderName+entity_identifier+"_message_"+(i+1)+".ser");
                    oos = new ObjectOutputStream(fos);
                    MessageType message = current_messages.get(i);
                    System.out.println(message);
                    oos.writeObject(message);

                    System.out.println("Message "+(i+1)+"/"+
                            current_messages.size()+
                            " for Entity "+entity_identifier+
                            " serialized and saved to file.");
                }
                System.out.println("All messages for Entity "+entity_identifier+" serialized and saved to file");

                it.remove(); // avoids a ConcurrentModificationException
            }

            // closing resources
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
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
