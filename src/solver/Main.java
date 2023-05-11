package solver;

import solver.complete.IPModel;
import solver.incomplete.BestImprovement;
import solver.util.Timer;

import java.nio.file.Path;
import java.nio.file.Paths;


public class Main {
    static int NUM_TRIES = 10;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java Main <file>");
            return;
        }

        String input = args[0];
        Path path = Paths.get(input);
        String filename = path.getFileName().toString();
        System.out.println("Instance: " + input);

        Timer watch = new Timer();
        VRPInstance instance = new VRPInstance(input);
        double objVal = Double.POSITIVE_INFINITY;
        String solution;
        watch.start();
        // Complete algorithm if numCustomers low enough
        if (instance.numCustomers < 10) {
            IPModel ipModel = new IPModel(instance);
            objVal = ipModel.solve();
            solution = ipModel.solutionToString();
            ipModel.solutionToFile(filename);
        } else {
            for (int i = 0; i < NUM_TRIES; i++) {
                BestImprovement solver = new BestImprovement(instance, 5);
                double currObjVal = solver.solve();
                if (currObjVal < objVal) {
                    objVal = currObjVal;
                    solution = solver.bestSolutionToString();
                    solver.bestSolutionToFile(filename);
                }
            }
        }
        watch.stop();

        System.out.println("{\"Instance\": \"" + filename +
                "\", \"Time\": " + String.format("%.2f",watch.getTime()) +
                ", \"Result\": " + String.format("%.2f", objVal) +
                ", \"Solution\": \"" + solution +  "\"}");

    }
}