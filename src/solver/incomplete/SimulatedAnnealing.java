package solver.incomplete;

import solver.VRPInstance;
import solver.util.Node;
import solver.util.Route;
import solver.util.Timer;

import java.io.FileNotFoundException;

public class SimulatedAnnealing extends LocalSearcher {
    double temperature, alpha;

    public SimulatedAnnealing(VRPInstance instance, int solveTime, double temperature, double alpha) throws FileNotFoundException {
        super(instance, solveTime);
        this.temperature = temperature;
        this.alpha = alpha;
    }

    public boolean step() throws Exception {
        // System.out.println("\nStep:");
        
        while (true) {
            Proposed move = proposeRandomMove();
            if (move.delta <= 0 || generator.nextDouble() < (Math.exp(-move.delta / this.temperature))) {
                double cost = 0.0;
                if (move.move == 0) {
                    cost = relocate(move.n1, move.n2);
                } else if (move.move == 1) {
                    cost = swap(move.n1, move.n2);
                }
                currObjVal += cost;
                checker.check(vrp, vehicleRoutes);
                this.temperature *= this.alpha;
                // System.out.println(this.temperature);
                return false;
            }
        }
    }

    public double solve() throws Exception {
        Timer timer = new Timer();
        timer.start();
        int i = 0;
        boolean localMin;
        while (timer.getTime() < runtime && i < numIter) {
            // System.out.println(timer.getTime());
            i++;
            step();
            if (currObjVal < bestObjVal) {
                bestObjVal = currObjVal;
                bestRoutes = vehicleRoutes;
            }
        }
        return bestObjVal;
    }


}

