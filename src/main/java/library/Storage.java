package library;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Storage {

    private static final String FOLDER = "storage_folder";
    private static final String SEP = ",";
    private static final String SLASH = "/";
    private static final String EXTENSION = ".csv";


    public static void createFolder() {
        try {
            Path path = Paths.get(FOLDER);
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }
        } catch (Exception e) {
            System.err.println("Could not create folder");
        }
    }

    public static void writeFile( Node node) {
        String fileName = buildFileName(node.getId());

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)))) {
            //TODO: get the snapshot
            Snapshot snapshot = node.getSnapshot();
            String rowToAdd = buildRow(snapshot);
            writer.println(rowToAdd);
            System.out.println("Snapshot is saved.");
        } catch (Exception e) {
            System.err.println("Could not write file ");
        }
    }



    public static void deleteFile(int id) {
        try {
            Path path = Paths.get(buildFileName(id));
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch (Exception e) {
            System.err.println("Could not delete file");
        }
    }

    private static String buildFileName(int id) {
        String fileName = FOLDER + SLASH + id + EXTENSION;
        return fileName;
    }

    private static String buildRow(Snapshot snapshot) {
        // TODO: need to identify the node
        String row = snapshot.getCurrentAmount() + SEP + snapshot.getCurrentAmount();
        return row;
    }



}