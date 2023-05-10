package solver;

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

        VRPInstance instance = new VRPInstance(input);
        Timer watch = new Timer();
        watch.start();
        BestImprovement solver = new BestImprovement(instance, 180);
        double result = solver.solve();
        watch.stop();

        System.out.println("{\"Instance\": \"" + filename +
                "\", \"Time\": " + String.format("%.2f",watch.getTime()) +
                ", \"Result\": " + String.format("%.2f", result) +
                ", \"Solution\": \"" + solver.bestSolutionToString() +  "\"}");

        solver.bestSolutionToFile(filename);
    }
}