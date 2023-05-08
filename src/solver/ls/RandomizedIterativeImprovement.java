package solver.ls;

import solver.VRPInstance;

public class RandomizedIterativeImprovement extends LocalSearcher {

    public RandomizedIterativeImprovement(VRPInstance instance, int solveTime) {
        super(instance, solveTime);
    }

    public void step() {

    }

    // Random node choice
    public Node chooseRelocationNode() {
        return customerNodes[(int) Math.floor(generator.nextDouble() * customerNodes.length)];
    }

    public void searchNeighborhood() throws Exception {
        Node n = chooseRelocationNode();
        Node newLocPrev = bestRelocation(n);
        if (newLocPrev != null) {
            relocate(n, newLocPrev);
        } else {
            System.out.println("No feasible relocation found.");
        }
    }

    public Node bestRelocation(Node n) {
        Node bestRelocatePrev = null;
        double bestDiff = Double.POSITIVE_INFINITY;
        for (int v = 0; v < vrp.numVehicles; v++) {
            Node depot = vehicleRoutes[v].route.depot;
            Node curr = depot;
            do {
                if (checkRelocationFeasibility(v, n.val)) {
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

}
