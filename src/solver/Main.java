package solver;

import solver.complete.IPModel;
import solver.incomplete.BestImprovement;
import solver.incomplete.DisturbedBestImprovement;
import solver.incomplete.RandomizedIteratedImprovement;
import solver.incomplete.SimulatedAnnealing;
import solver.util.Timer;

import java.nio.file.Path;
import java.nio.file.Paths;


// TODO: RUN WITH CHECKER BEFORE SUBMISSION

public class Main {
    static int TOTAL_RUNTIME = 269;
    static int INTERNAL_RUNTIME = 30;

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
        String solution = "";
        watch.start();
        // Complete algorithm if numCustomers low enough
        if (instance.numCustomers < 20) {
            IPModel ipModel = new IPModel(instance);
            objVal = ipModel.solve();
            solution = ipModel.solutionToString();
            ipModel.solutionToFile(filename);
        } else {
            Timer timer = new Timer();
            timer.start();
            // while (timer.getTime() < TOTAL_RUNTIME) {
                // SimulatedAnnealing solver = new SimulatedAnnealing(instance, INTERNAL_RUNTIME, 1.0, 0.999999);
                // BestImprovement solver = new BestImprovement(instance, INTERNAL_RUNTIME);
                // RandomizedIteratedImprovement solver = new RandomizedIteratedImprovement(instance, INTERNAL_RUNTIME);
                DisturbedBestImprovement solver = new DisturbedBestImprovement(instance, INTERNAL_RUNTIME);
                double currObjVal = solver.solve();
                if (currObjVal < objVal) {
                    objVal = currObjVal;
                    solution = solver.bestSolutionToString();
                    solver.bestSolutionToFile(filename);
                }
            // }
        }
        watch.stop();

        System.out.println("{\"Instance\": \"" + filename +
                "\", \"Time\": " + String.format("%.2f",watch.getTime()) +
                ", \"Result\": " + String.format("%.2f", objVal) +
                ", \"Solution\": \"" + solution +  "\"}");

    }
}