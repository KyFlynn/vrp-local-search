package solver.incomplete;

import solver.VRPInstance;
import solver.util.Node;
import solver.util.Pair;
import solver.util.Timer;

import java.io.FileNotFoundException;

public class DisturbedBestImprovement extends BestImprovement {

    int numDisturbances = 2;
    double prevLocalMinVal;
    Timer timer = new Timer();

    public DisturbedBestImprovement(VRPInstance instance, int solveTime) throws FileNotFoundException {
        super(instance, solveTime);
    }

    // Returns whether local minimum has been reached
    public boolean step() throws Exception {
        Node[] nodeOrder = randomCustomerOrdering(customerNodes);
        Node choice = null;
        Pair<Node, Integer> bestMove = null;
        for (Node n : nodeOrder) {
            Pair<Node, Integer> move = searchNeighborhood(n);
            if (move.x != null) {
                bestMove = move;
                choice = n;
                break;
            }
        }
        if (bestMove != null) {
            makeMove(choice, bestMove);
            // TODO: Remember to turn on checker below before submission
            // return !(checker.check(vrp, vehicleRoutes));
            return false;
        }
        return true;
    }

    public void updateDisturbanceNumber() {
        numDisturbances *= 2;
    }

    public void applyDisturbances() throws Exception {
        // Applying numDisturbances disturbances to current solution
        for (int i = 0; i < numDisturbances; i++) {
            Proposed randomMove = proposeRandomMove();
            Pair<Node, Integer> move = new Pair<>(randomMove.n2, randomMove.move);
            makeMove(randomMove.n1, move);
        }
    }

    public void disturb() throws Exception {
        // System.out.println(String.format("Local min reached at %.4f. Restarting with %d disturbances.",
        //         currObjVal, numDisturbances));
        // Update previous local min value
        prevLocalMinVal = currObjVal;
        do {
            applyDisturbances();
            // Reach local minimum again
            boolean localMin = false;
            while (!localMin & timer.getTime() < runtime) {
                localMin = step();
                // Update best
                if (currObjVal < bestObjVal) {
                    bestObjVal = currObjVal;
                    bestRoutes = vehicleRoutes;
                }
            }
            // Check if same as last local minimum -> not enough disturbances
            if (currObjVal == prevLocalMinVal) {
                updateDisturbanceNumber();
            }
        } while (currObjVal == prevLocalMinVal & timer.getTime() < runtime);
    }

    public double solve() throws Exception {
        timer.start();
        boolean localMin;
        while (timer.getTime() < runtime) {
            localMin = step();
            // Update best
            if (currObjVal < bestObjVal) {
                bestObjVal = currObjVal;
                bestRoutes = vehicleRoutes;
            }
            // If at local min, begin disturbance
            if (localMin) {
                disturb();
            }
        }
        return bestObjVal;
    }


}
