package solver;
import solver.construction.savings.SavingsAlgorithm;

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
        // IPModel ipModel = new IPModel(instance);
        SavingsAlgorithm savingsModel = new SavingsAlgorithm(instance);

        watch.start();
        String solution = savingsModel.run();
        // double objVal = ipModel.solve();
        watch.stop();

        // System.out.println(ipModel.printSolution());
       System.out.println("{\"Instance\": \"" + filename +
               "\", \"Time\": " + String.format("%.2f",watch.getTime()) +
               ", \"Result\": " + String.format("%.2f", savingsModel.get_cost()) + "" +
               ", \"Solution\": \"" + solution +  "\"}");
    }
}