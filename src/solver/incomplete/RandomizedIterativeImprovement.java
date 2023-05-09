package solver.incomplete;

import solver.VRPInstance;
import solver.util.Node;

import java.util.Arrays;
import java.util.Collections;

public class RandomizedIterativeImprovement extends LocalSearcher {

    double randomizeProb;

    public RandomizedIterativeImprovement(VRPInstance instance, int solveTime) {
        super(instance, solveTime);
        assert randomizeProb >= 0 && randomizeProb <= 1;
    }


    public Node bestRelocation(Node n) {
        Node bestRelocatePrev = null;
        double bestDiff = Double.POSITIVE_INFINITY;
        for (int v = 0; v < vrp.numVehicles; v++) {
            Node depot = vehicleRoutes[v].route.depot;
            Node curr = depot;
            do {
                if (checkRelocationFeasibility(v, n)) {
                    double score = relocationScore(n, curr);
                    if (score < bestDiff) {
                        bestRelocatePrev = curr;
                        bestDiff = score;
                    }
                }
            } while (curr.next != depot);
        }
        return bestRelocatePrev;
    }

    public void searchNeighborhood(Node n) throws Exception {
        Node newLocPrev = bestRelocation(n);
        if (newLocPrev != null) {
            relocate(n, newLocPrev);
        } else {
            System.out.println("No feasible relocation found.");
        }
    }

    public void step() throws Exception {
        Node[] nodeOrder = randomCustomerOrdering(customerNodes);
        for (Node n : nodeOrder) {
            if (checkRelocationFeasibility(n.vehicle, n)) {
//                double rand = generator.nextDouble();
                searchNeighborhood(n);
//                if (rand > randomizeProb) {
//                    // TODO: Implement random move
//                    //  searchNeighborhood(n);
//                } else {
//                    searchNeighborhood(n);
//                }
            } else{
                System.out.println("No feasible move found.");
                return;
            }
        }
        checker.check(vrp, vehicleRoutes);
    }

}
