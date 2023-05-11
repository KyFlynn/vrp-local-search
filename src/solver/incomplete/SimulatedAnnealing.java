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
        System.out.println("\nStep:");
        
        while (true) {
            Proposed move = proposeRandomMove();
            if (move.improving || generator.nextDouble() < this.temperature) {
                if (move.move == 0) {
                    relocate(move.n1, move.n2);
                } else if (move.move == 1) {
                    swap(move.n1, move.n2);
                }
                checker.check(vrp, vehicleRoutes);
                this.temperature *= this.alpha;
                return true;
            }
        }
    }

    public double solve() throws Exception {
        Timer timer = new Timer();
        timer.start();
        int i = 0;
        boolean localMin;
        while (timer.getTime() < runtime && i < numIter) {
            i++;
            localMin = step();
            if (currObjVal < bestObjVal) {
                bestObjVal = currObjVal;
                bestRoutes = vehicleRoutes;
            }
            if (localMin) {
                System.out.println("Local minimum reached.");
                break;
            }
        }
        return bestObjVal;
    }


}

