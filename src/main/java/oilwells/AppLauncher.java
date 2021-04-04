package oilwells;

public class AppLauncher {
    private static OilWell oilWell = new OilWell();

    public static void main(String[] args) {
        System.out.println("Welcome to the oil-wells system!");
        System.out.println("Type in: action name, hostname, port, (oilAmount)");
        System.out.println("Example: initialize, localhost, 10000, 1000");
        System.out.println("Example: connect, localhost, 10001");
        System.out.println("Example: snapshot");
        Parser.parseInput(OilWell.class.getName(), oilWell);
    }
}
