package solver.incomplete;

import solver.VRPInstance;
import solver.util.Node;
import solver.util.Pair;
import solver.util.Timer;

import java.io.FileNotFoundException;

public class DisturbedBestImprovement extends BestImprovement {

    int initNumDisturbances = 4;
    int maxNumDisturbances = 50000;
    int numCustomerGrowthFactor = 400;
    double disturbGrowthFact;
    double prevLocalMinVal;
    Timer timer = new Timer();

    public DisturbedBestImprovement(VRPInstance instance) throws FileNotFoundException {
        super(instance);
        disturbGrowthFact = 1.1 + (instance.numCustomers / numCustomerGrowthFactor);
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
            return false;
        }
        return true;
    }

    public int updateDisturbanceNumber(int numDisturbances) {
        return (int) Math.min(Math.ceil(numDisturbances * disturbGrowthFact), maxNumDisturbances);
    }

    public void applyDisturbances(int numDisturbances) throws Exception {
        // Applying numDisturbances disturbances to current solution
        for (int i = 0; i < numDisturbances; i++) {
            Proposed randomMove = proposeRandomMove();
            Pair<Node, Integer> move = new Pair<>(randomMove.n2, randomMove.move);
            makeMove(randomMove.n1, move);
        }
    }

    public void disturb(int numDisturbances) throws Exception {
        // System.out.println(String.format("Local min reached at %.4f. Restarting with %d disturbances.",
        //         currObjVal, numDisturbances));
        // Update previous local min value
        prevLocalMinVal = currObjVal;
        do {
            applyDisturbances(numDisturbances);
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
                numDisturbances = updateDisturbanceNumber(numDisturbances);
            }
        } while (currObjVal == prevLocalMinVal & timer.getTime() < runtime);
    }

    public double solve() throws Exception {
        setup();
        
        timer.start();
        boolean localMinReached;
        while (timer.getTime() < runtime) {
            localMinReached = step();
            // Update best
            if (currObjVal < bestObjVal) {
                bestObjVal = currObjVal;
                bestRoutes = vehicleRoutes;
            }
            // If at local min, begin disturbance
            if (localMinReached) {
                disturb(initNumDisturbances);
            }
        }
        return bestObjVal;
    }


}
