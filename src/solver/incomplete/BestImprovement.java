package solver.incomplete;

import solver.VRPInstance;
import solver.util.Node;
import solver.util.Pair;
import solver.util.Route;
import solver.util.Timer;

import java.io.FileNotFoundException;

public class BestImprovement extends LocalSearcher {


    public BestImprovement(VRPInstance instance, int solveTime) throws FileNotFoundException {
        super(instance, solveTime);
    }

    public Pair<Node, Double> findBestRelocation(Node n) {
        // System.out.println(String.format("Searching relocations for vehicle %d, customer %d", n.vehicle, n.customer));
        Node bestRelocatePrev = null;
        double bestCost = -1 * 10e-10;
        // Relocation neighborhood search
        for (int v = 0; v < vrp.numVehicles; v++) {
            // Check if relocating customer to this vehicle is feasible.
            if (checkRelocationFeasibility(v, n)) {
                Node depot = vehicleRoutes[v].routeCycle.depot;
                Node curr = depot;
                // Go through possible relocation positions in vehicle's route.
                do {
                    // Ignore certain relocation positions -> (same vehicle AND (n's previous or same as n or tour of 3)).
                    if (n.vehicle == curr.vehicle && (curr == n.prev || curr == n || n.prev == n.next.next)) {
                        curr = curr.next;
                        continue;
                    }
                    double cost = relocationCost(n, curr);
                    // System.out.println(String.format("Relocation: Vehicle %d, PrevNode: %d, Cost: %.2f", v, curr.customer, cost));
                    if (cost < bestCost) {
                        bestRelocatePrev = curr;
                        bestCost = cost;
                    }
                    curr = curr.next;
                } while (curr.next != depot);
            }
        }
        return new Pair<>(bestRelocatePrev, bestCost);
    }

    public Pair<Node, Double> findBestSwap(Node n) {
        // System.out.println(String.format("Searching swaps for vehicle %d, customer %d", n.vehicle, n.customer));
        Node bestSwapNode = null;
        double bestCost = -1 * 10e-10;
        // Swapping neighborhood search
        for (int v = 0; v < vrp.numVehicles; v++) {
            Node depot = vehicleRoutes[v].routeCycle.depot;
            // Start at node after depot - swapping with depot is impossible.
            Node curr = depot.next;
            // Go through possible customer swaps in vehicle's route.
            while (curr != depot) {
                // Ignore certain swapping positions  -> (same vehicle AND (same as n or tour of 3)).
                if (n.vehicle == curr.vehicle && (curr == n || n.prev == n.next.next)) {
                    curr = curr.next;
                    continue;
                }
                // Check if feasible.
                if (checkSwappingFeasibility(n.vehicle, curr.vehicle, n, curr)) {
                    double cost;
                    // Neighbors cases
                    if (n.next == curr) {
                        // !! Sets order to n1 -> n2 for all calls !!
                        cost = swappingNeighborsCost(n, curr);
                    } else if (curr.next == n) {
                        cost = swappingNeighborsCost(curr, n);
                    } else {
                        // Standard case
                        cost = swappingCost(n, curr);
                    }
                    // System.out.println(String.format("Swapping: Vehicle %d, Node: %d, Cost: %.2f", v, curr.customer, cost));
                    if (cost < bestCost) {
                        bestSwapNode = curr;
                        bestCost = cost;
                    }
                }
                curr = curr.next;
            }
        }
        return new Pair<>(bestSwapNode, bestCost);
    }

    public Pair<Node, Integer> searchNeighborhood(Node n) {
        Pair<Node, Double> bestLocalRelocate = findBestRelocation(n);
        Pair<Node, Double> bestLocalSwap = findBestSwap(n);
        int moveType = bestLocalRelocate.y < bestLocalSwap.y ? 0 : 1;
        switch (moveType) {
            case 0:
                return new Pair<>(bestLocalRelocate.x, 0);
            case 1:
                return new Pair<>(bestLocalSwap.x, 1);
            default:
                System.out.println("Default case reached.");
                return null;
        }
    }

    public void makeMove(Node n, Pair<Node, Integer> move) throws Exception {
        double cost = 0;
        switch (move.y) {
            case 0:
                // System.out.println(String.format("Relocating %d to: Vehicle %d, PrevNode: %d",
                //         n.customer, move.x.vehicle, move.x.customer));
                cost = relocate(n, move.x);
                break;
            case 1:
                // System.out.println(String.format("Swapping %d to: Vehicle %d, Node: %d",
                //         n.customer, move.x.vehicle, move.x.customer));
                cost = swap(n, move.x);
                break;
            default:
                System.out.println("Default case reached.");
                break;
        }
        // Update objective value
        currObjVal += cost;
    }

    // Returns whether local minimum has been reached
    public boolean step() throws Exception {
        // System.out.println("\nStep:");
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
            // System.out.println(currObjVal);
            // for (Route r : vehicleRoutes) {
            //     r.printRoute();
            // }
            // TODO: Remember to turn on checker below before submission
            // return !(checker.check(vrp, vehicleRoutes));
            return false;
        }
        return true;
    }

    public double solve() throws Exception {
        Timer timer = new Timer();
        timer.start();
        boolean localMin;
        while (timer.getTime() < runtime) {
            localMin = step();
            if (currObjVal < bestObjVal) {
                bestObjVal = currObjVal;
                bestRoutes = vehicleRoutes;
            }
            if (localMin) {
                System.out.println("Local minimum reached.");
                // for (Route r : vehicleRoutes) {
                //     r.printRoute();
                // }
                break;
            }
        }
        return bestObjVal;
    }


}

