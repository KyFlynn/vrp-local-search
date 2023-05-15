package solver;

import solver.complete.IPModel;
import solver.complete.IPModelLazy;
import solver.incomplete.*;
import solver.util.Node;
import solver.util.Route;
import solver.util.Timer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    static int TOTAL_RUNTIME = 285;
    static int INTERNAL_RUNTIME = 285;

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
        double bestObjVal = Double.POSITIVE_INFINITY;
        String solution = "";
        Route[] bestRoutes = null;
        watch.start();
        // Complete algorithm if numCustomers low enough
        if (instance.numCustomers < 20) {
            IPModel ipModel = new IPModel(instance);
            bestObjVal = ipModel.solve();
            solution = ipModel.solutionToString();
            ipModel.solutionToFile(filename);
            // Lazy constraint IP model -> failed terribly, way too slow :(
            // IPModelLazy ipModelLazy = new IPModelLazy(instance);
            // objVal = ipModelLazy.solve();
            // solution = ipModelLazy.solutionToString();
            // ipModelLazy.solutionToFile(filename);
        } else {
            // DisturbedBestImprovement solver = new DisturbedBestImprovement(instance);
            LocalExplorationDisturbedBestImprovement solver = new LocalExplorationDisturbedBestImprovement(instance);
            // SimulatedAnnealing solver = new SimulatedAnnealing(instance, 1.0, 0.999999);
            while (watch.getTime() < TOTAL_RUNTIME) {
                solver.setRuntime(Math.min(INTERNAL_RUNTIME, TOTAL_RUNTIME - (int) watch.getTime()));
                double currObjVal = solver.solve();
                if (currObjVal < bestObjVal) {
                    bestObjVal = currObjVal;
                    bestRoutes = solver.bestRoutes;
                }
                System.out.println(bestRoutes);
                System.out.println(bestObjVal);
            }
        }
        watch.stop();
        // Save solutions and turn into string if not IP model
        if (instance.numCustomers >= 20) {
            // Check solutions
            Checker checker = new Checker();
            boolean correct = checker.check(instance, bestRoutes);
            if (!correct) {
                throw new Exception("INCORRECT SOLUTION");
            }
            solution = solutionToString(bestRoutes);
            solutionToFile(bestObjVal, bestRoutes, filename);
        }

        // TODO: Change output
        System.out.println("{\"Instance\": \"" + filename +
                "\", \"Time\": " + String.format("%.2f",watch.getTime()) +
                ", \"Result\": " + String.format("%.2f", bestObjVal) +
                ", \"Solution\": \"" + solution +  "\"}");
    }


    // SOLUTION UTILS

    public static String solutionToString(Route[] routes) {
        String solution = "0 ";
        for (Route r : routes) {
            solution += "0 ";
            Node curr = r.routeCycle.depot.next;
            while (curr != r.routeCycle.depot) {
                solution += String.format("%d ", curr.customer);
                curr = curr.next;
            }
            solution += "0 ";
        }
        return solution.trim();
    }

    public static void solutionToFile(double objVal, Route[] routes, String filename) throws FileNotFoundException {
        PrintStream out = new PrintStream(new FileOutputStream("solutions/" + filename + ".sol"));
        String solution = String.format("%.2f 0", objVal);
        for (Route r : routes) {
            solution += "\n0 ";
            Node curr = r.routeCycle.depot.next;
            while (curr != r.routeCycle.depot) {
                solution += String.format("%d ", curr.customer);
                curr = curr.next;
            }
            solution += "0";
        }
        out.print(solution);
        out.flush();
        out.close();
    }
}