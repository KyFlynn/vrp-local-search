package solver;

import solver.incomplete.RandomizedIterativeImprovement;
import solver.initial.savings.SavingsAlgorithm;
import solver.initial.sweep.SweepAlgorithm;
import solver.initial.randomfeasible.CPSolutionFinder;
import solver.util.Timer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

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
        // IPModel ipModel = new IPModel(instance);
        // SavingsAlgorithm savingsModel = new SavingsAlgorithm(instance);
        // SweepAlgorithm sweepModel = new SweepAlgorithm(instance);
        // CPSolutionFinder cpModel = new CPSolutionFinder(instance);

        watch.start();
        RandomizedIterativeImprovement solver = new RandomizedIterativeImprovement(instance, 5);
        double res = solver.solve();
        // ArrayList<ArrayList<Integer>> res = savingsModel.run();
        // if (res == null) {
        //     System.out.println("LKDHFSKDJHFLJSDHF");
        // } else {
        //     System.out.println("ok");
        // }
        // double objVal = ipModel.solve();
        // sweepModel.run();
        // ArrayList<ArrayList<Integer>> res = cpModel.run();
        watch.stop();

        System.out.println(res);

        // System.out.println(ipModel.printSolution());
    //    System.out.println("{\"Instance\": \"" + filename +
    //            "\", \"Time\": " + String.format("%.2f",watch.getTime()) +
    //            ", \"Result\": " + String.format("%.2f", savingsModel.get_cost()) + "" +
    //            ", \"Solution\": \"" + solution +  "\"}");
    }
}