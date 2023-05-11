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
        System.out.println(String.format("Searching relocations for vehicle %d, customer %d", n.vehicle, n.customer));
        Node bestRelocatePrev = null;
        double bestCost = 0;
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
                    System.out.println(String.format("Relocation: Vehicle %d, Prev: %d, Cost: %.2f", v, curr.customer, cost));
                    if (cost <= bestCost) {
                        bestRelocatePrev = curr;
                        bestCost = cost;
                    }
                    curr = curr.next;
                } while (curr.next != depot);
            }
        }
        return new Pair(bestRelocatePrev, bestCost);
    }

    public Pair<Node, Double> findBestSwap(Node n) {
        System.out.println(String.format("Searching swaps for vehicle %d, customer %d", n.vehicle, n.customer));
        Node bestSwapNode = null;
        double bestCost = 0;
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
                    double cost = swappingCost(n, curr);
                    System.out.println(String.format("Swapping: Vehicle %d, Prev: %d, Cost: %.2f", v, curr.customer, cost));
                    if (cost <= bestCost) {
                        bestSwapNode = curr;
                        bestCost = cost;
                    }
                }
                curr = curr.next;
            }
        }
        return new Pair(bestSwapNode, bestCost);
    }

    public Pair<Node, Integer> searchNeighborhood(Node n) {
        Pair bestLocalRelocate = findBestRelocation(n);
        Pair bestLocalSwap = findBestSwap(n);
        int moveType = (double) bestLocalRelocate.y < (double) bestLocalSwap.y ? 0 : 1;
        switch (moveType) {
            case 0:
                return new Pair(bestLocalRelocate, 0);
            case 1:
                return new Pair(bestLocalSwap, 0);
        }
    }

    // Returns whether local maximum has been reached
    public boolean step() throws Exception {
        System.out.println("\nStep:");
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
                case 1:
                    cost = swap(choice, bestMove.x);
            }
            System.out.println(currObjVal);
            currObjVal += cost;
            for (Route r : vehicleRoutes) {
                r.printRoute();
            }
            return !(checker.check(vrp, vehicleRoutes));
        }
        return true;
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

