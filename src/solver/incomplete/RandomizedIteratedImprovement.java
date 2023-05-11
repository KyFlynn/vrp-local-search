package solver.incomplete;


import solver.VRPInstance;
import solver.util.Node;
import solver.util.Route;
import solver.util.Timer;
import solver.util.Pair;

import java.io.FileNotFoundException;

public class RandomizedIteratedImprovement extends BestImprovement {
    double clownFactor;

    public RandomizedIteratedImprovement(VRPInstance instance, int solveTime, double clownFactor) throws FileNotFoundException {
        super(instance, solveTime);
        this.clownFactor = clownFactor;
    }

    // Returns whether local maximum has been reached
    public boolean step() throws Exception {
        System.out.println("\nStep:");
        // Maybe do a random step??
        if (generator.nextDouble() < this.clownFactor) {
            Proposed move = proposeRandomMove();
            if (move.move == 0) {
                relocate(move.n1, move.n2);
            } else if (move.move == 1) {
                swap(move.n1, move.n2);
            }
            checker.check(vrp, vehicleRoutes);
            return false;
        }

        // If we didn't take a random step, do a normal step
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
        double cost = 0;
        if (bestMove.x != null) {
            switch (bestMove.y) {
                case 0:
                    cost = relocate(choice, bestMove.x);
                    break;
                case 1:
                    cost = swap(choice, bestMove.x);
                    break;
            }
            System.out.println(currObjVal);
            currObjVal += cost;
            for (Route r : vehicleRoutes) {
                r.printRoute();
            }
            return !(checker.check(vrp, vehicleRoutes));
        }
        return false;
    }
}

