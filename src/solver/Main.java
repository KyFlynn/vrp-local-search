package solver;

import ilog.concert.IloException;

import java.io.FileNotFoundException;
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
        IPModel ipModel = new IPModel(instance);

        watch.start();
        double objVal = ipModel.solve();
        watch.stop();

        System.out.println(ipModel.printSolution());
    }
}