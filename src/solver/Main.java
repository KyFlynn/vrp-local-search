package solver;

import solver.complete.IPModel;
import solver.incomplete.BestImprovement;
import solver.util.Timer;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
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
        double objVal = -1.0;
        String solution = "";
        watch.start();
        // Complete algorithm if numCustomers low enough
        if (instance.numCustomers < 30) {
            IPModel ipModel = new IPModel(instance);
            objVal = ipModel.solve();
            solution += ipModel.solutionToString();
            ipModel.solutionToFile(filename);
        } else {
            BestImprovement solver = new BestImprovement(instance, 5);
            objVal = solver.solve();
            solution += solver.bestSolutionToString();
            solver.bestSolutionToFile(filename);
        }
        watch.stop();

        System.out.println("{\"Instance\": \"" + filename +
                "\", \"Time\": " + String.format("%.2f",watch.getTime()) +
                ", \"Result\": " + String.format("%.2f", objVal) +
                ", \"Solution\": \"" + solution +  "\"}");

    }
}