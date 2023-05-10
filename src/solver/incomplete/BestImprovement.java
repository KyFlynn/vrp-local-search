package solver.incomplete;

import solver.VRPInstance;
import solver.util.Node;
import solver.util.Route;
import solver.util.Timer;

public class BestImprovement extends LocalSearcher {


    public BestImprovement(VRPInstance instance, int solveTime) {
        super(instance, solveTime);
    }

    public Node findBestNeighborhoodMove(Node n) {
        Node bestRelocatePrev = null;
        double bestCost = 0;
        for (int v = 0; v < vrp.numVehicles; v++) {
            // Check if relocating customer to this vehicle is feasible.
            if (checkRelocationFeasibility(v, n)) {
                Node depot = vehicleRoutes[v].routeCycle.depot;
                Node curr = depot;
                // Go through possible relocation positions in vehicle's route.
                do {
                    // Ignore current positions of node n -> (same vehicle AND (n's previous or n or tour of 3)).
                    if (n.vehicle == curr.vehicle && (curr == n.prev || curr == n || n.prev == n.next.next)) {
                        curr = curr.next;
                        continue;
                    }
                    double cost = relocationCost(n, curr);
                    if (cost <= bestCost) {
                        bestRelocatePrev = curr;
                        bestCost = cost;
                    }
                    curr = curr.next;
                } while (curr.next != depot);
            }
        }
        return bestRelocatePrev;
    }

    // Returns whether local maximum has been reached
    public boolean step() throws Exception {
        System.out.println("\nStep:");
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
            checker.check(vrp, vehicleRoutes);
            return false;
        }
        return true;
    }

    public double solve() throws Exception {
        Timer timer = new Timer();
        timer.start();
        int i = 0;
        boolean localMin;
        while(timer.getTime() < runtime && i < numIter) {
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

