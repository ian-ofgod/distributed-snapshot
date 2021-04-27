package library;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

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
     * */
    private static void createFolder() {
        try {
            Path path = Paths.get(FOLDER);
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }
        } catch (Exception e) {
            System.err.println("Could not create folder");
        }
    }

    /**
     * Method to save a snapshot portion on disk.
     * @param <MessageType> this is the type that will be exchanged as a message between nodes
     * @param <StateType> this is the type that will be saved as the state of the application
     * @param runningSnapshots the list of snapshots running on the current node
     * @param snapshotId the id of the snapshot that the user want to save on disk
     * */
    public static <StateType, MessageType> void writeFile(ArrayList<Snapshot<StateType, MessageType>> runningSnapshots, int snapshotId) {
        createFolder();
        Snapshot<StateType, MessageType> toSaveSnapshot = runningSnapshots.stream().filter(snap -> snap.snapshotId==snapshotId).findFirst().orElse(null);
        String fileName = buildFileName(toSaveSnapshot);
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)))) {
            writer.println(toSaveSnapshot);
            System.out.println("Snapshot is saved.");
        } catch (Exception e) {
            System.err.println("Could not write file ");
        }
    }

    /**
     * Method to build the file name of the current snapshot before saving on disk.
     * @param <MessageType> this is the type that will be exchanged as a message between nodes
     * @param <StateType> this is the type that will be saved as the state of the application
     * @param snapshot the snapshot for which the name is built, will be saved on disk
     * */
    private static <StateType, MessageType> String buildFileName(Snapshot<StateType, MessageType> snapshot) {
        return FOLDER + "/" + snapshot.snapshotId + EXTENSION;
    }





}
