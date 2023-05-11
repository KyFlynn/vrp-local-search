package solver.incomplete;

import solver.VRPInstance;
import solver.util.Node;
import solver.util.Route;
import solver.util.Timer;

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
            return true;
        }

        // If we didn't take a random step, do a normal step
        Node[] nodeOrder = randomCustomerOrdering(customerNodes);
        Node choice = null;
        Node newLocPrev = null;
        for (Node n : nodeOrder) {
            newLocPrev = findBestNeighborhoodMove(n);
            if (newLocPrev != null) {
                choice = n;
                break;
            }
        }
        if (choice != null) {
            double relocateCost = relocate(choice, newLocPrev);
            System.out.println(currObjVal);
            currObjVal += relocateCost;
            for (Route r : vehicleRoutes) {
                r.printRoute();
            }
            return !(checker.check(vrp, vehicleRoutes));
        }
        return true;
    }
}

