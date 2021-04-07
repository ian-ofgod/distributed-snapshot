package library;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

class Storage {

    private static final String FOLDER = "storage_folder";
    private static final String SEP = ",";
    private static final String SLASH = "/";
    private static final String EXTENSION = ".data";


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

    public static void writeFile(ArrayList<Snapshot> runningSnapshots, int snapshotId) {
        createFolder();
        Snapshot toSaveSnapshot = runningSnapshots.stream().filter(snap -> snap.snapshotId==snapshotId).findFirst().orElse(null);
        String fileName = buildFileName(toSaveSnapshot);
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)))) {
            writer.println(toSaveSnapshot);
            System.out.println("Snapshot is saved.");
        } catch (Exception e) {
            System.err.println("Could not write file ");
        }
    }

    private static String buildFileName(Snapshot snapshot) {
        return FOLDER + SLASH + snapshot + EXTENSION;
    }





}
