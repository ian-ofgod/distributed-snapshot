package oilwells;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Scanner;

public class Parser {


    public static void parseInput(String className) {
        Scanner input = new Scanner(System.in);
        while (input.hasNext()) {
            String[] inputs = input.nextLine().split(",");
            String methodName = inputs[0];
            Object[] parameters = Arrays.copyOfRange(inputs, 1, inputs.length);

            Class<?>[] methodParameterTypes;

            switch (methodName) {
                case "initialize":
                    methodParameterTypes = new Class<?>[]{String.class, int.class, int.class};
                    parameters[1] = inputs[1];
                    parameters[2] = Integer.parseInt(inputs[2]);
                    parameters[3] = Integer.parseInt(inputs[3]);
                    break;
                case "connect":
                    methodParameterTypes = new Class<?>[]{String.class, int.class};
                    parameters[1] = inputs[1];
                    parameters[2] = Integer.parseInt(inputs[2]);
                    break;
                case "snapshot":
                    methodParameterTypes = new Class<?>[]{String.class};
                    parameters[1] = inputs[1];
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + methodName);
            }

            try {
                    Class.forName(className).getMethod(methodName, methodParameterTypes).invoke(null, parameters);
                } catch (Exception e) {
                    System.err.println("Unable to parse input");
                }
            }

        }
    }
