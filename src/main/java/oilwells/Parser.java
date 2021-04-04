package oilwells;

import java.util.Scanner;

public abstract class Parser {
    public static void parseInput(String className) {
        Scanner input = new Scanner(System.in);
        while (input.hasNext()) {
            String[] inputs = input.nextLine().split(", ");
            String methodName = inputs[0];
            Object[] parameters = new Object[inputs.length - 1];

            Class<?>[] methodParameterTypes;

            switch (methodName) {
                case "initialize":
                    methodParameterTypes = new Class<?>[]{String.class, int.class, int.class};
                    parameters[0] = inputs[1];
                    parameters[1] = Integer.parseInt(inputs[2]);
                    parameters[2] = Integer.parseInt(inputs[3]);
                    break;
                case "connect":
                    methodParameterTypes = new Class<?>[]{String.class, int.class};
                    parameters[0] = inputs[1];
                    parameters[1] = Integer.parseInt(inputs[2]);
                    break;
                case "snapshot":
                    methodParameterTypes = new Class<?>[]{String.class};
                    parameters[0] = inputs[1];
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + methodName);
            }

            try {
                    Class.forName(className).getMethod(methodName, methodParameterTypes).invoke(null, parameters);
                } catch (Exception e) {
                    System.err.println("Unable to parse input: " + e.getMessage());
                }
            }

        }
    }
