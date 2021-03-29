package library.utils;

import library.Node;

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
        String fileName = buildFileName(node.getIp_address(),node.getPort());

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)))) {
            //TODO: get the snapshot from Node
            Snapshot snapshot = new Snapshot(); // node.getSnapshot();
            String rowToAdd = buildRow(snapshot);
            writer.println(rowToAdd);
            System.out.println("Snapshot is saved.");
        } catch (Exception e) {
            System.err.println("Could not write file ");
        }
    }



    public static void deleteFile(String ip, int port) {
        try {
            Path path = Paths.get(buildFileName(ip, port));
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch (Exception e) {
            System.err.println("Could not delete file");
        }
    }

    private static String buildFileName(String ip, int port) {
        String fileName = FOLDER + SLASH + ip+"_"+port + EXTENSION;
        return fileName;
    }

    private static String buildRow(Snapshot snapshot) {
        String row =  snapshot.getSnapshotIdentifier() + SEP + snapshot.getCurrentAmount() + SEP + snapshot.getCurrentAmount();
        return row;
    }



}
