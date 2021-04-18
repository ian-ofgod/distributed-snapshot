package oilwells;

import java.util.Scanner;

/**
 * It's used to parse the user input from the command line
 * */
public abstract class Parser {
    public static void parseInput(String className, Object classObject) {
        Scanner input = new Scanner(System.in);
        while (input.hasNext()) {
            String[] inputs = input.nextLine().split(", ");
            String methodName = inputs[0];
            Object[] parameters = new Object[inputs.length - 1];

            Class<?>[] methodParameterTypes;

            switch (methodName) {
                case "initialize" -> {
                    methodParameterTypes = new Class<?>[]{String.class, int.class, int.class};
                    if (parameters.length != 3) throw new IllegalStateException("Unexpected number of parameters");
                    parameters[0] = inputs[1];
                    parameters[1] = Integer.parseInt(inputs[2]);
                    parameters[2] = Integer.parseInt(inputs[3]);
                }
                case "disconnect", "connect" -> {
                    methodParameterTypes = new Class<?>[]{String.class, int.class};
                    if (parameters.length != 2) throw new IllegalStateException("Unexpected number of parameters");
                    parameters[0] = inputs[1];
                    parameters[1] = Integer.parseInt(inputs[2]);
                }
                case "snapshot" -> {
                    if (parameters.length != 0) throw new IllegalStateException("Unexpected number of parameters");
                    methodParameterTypes = new Class<?>[]{};
                }
                default -> throw new IllegalStateException("Unexpected value: " + methodName);
            }

            try {
                Class.forName(className).getMethod(methodName, methodParameterTypes).invoke(classObject, parameters);
            } catch (Exception e) {
                throw new IllegalStateException("Something went wrong calling " + methodName);
            }
        }
    }
}
