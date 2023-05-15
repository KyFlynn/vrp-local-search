package solver.incomplete;

import solver.VRPInstance;
import solver.initial.randomfeasible.CPSolutionFinder;
import solver.initial.savings.SavingsAlgorithm;
import solver.initial.sweep.SweepAlgorithm;
import solver.util.Node;
import solver.util.Route;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Random;

public abstract class LocalSearcher {

    VRPInstance vrp;
    public double runtime;
    Random generator = new Random();  // TODO: Find seed
    Node[] customerNodes;
    Route[] vehicleRoutes;
    double currObjVal;
    public Route[] bestRoutes;
    public double bestObjVal;
    Checker checker = new Checker();
    int numIter = Hyperparameters.numIter;

    SavingsAlgorithm savings;
    SweepAlgorithm sweep;
    CPSolutionFinder cp;

    public LocalSearcher(VRPInstance instance) {
        vrp = instance;
        customerNodes = new Node[vrp.numCustomers - 1];
        for (int i = 0; i < vrp.numCustomers - 1; i++) {
            customerNodes[i] = new Node(null, i + 1, -1, null);
        }
        this.savings = new SavingsAlgorithm(vrp);
        this.sweep = new SweepAlgorithm(vrp);
        this.cp = new CPSolutionFinder(vrp);
    }

    public void setRuntime(double time) {
        runtime = time;
    }

    public void setup() {
        ArrayList<ArrayList<Integer>> initialRoutes = initSolution();
        vehicleRoutes = new Route[vrp.numVehicles];
        initRoutes(initialRoutes);
        currObjVal = 0;
        for (int v = 0; v < vrp.numVehicles; v++) {
            currObjVal += vehicleRoutes[v].distance;
        }
        bestRoutes = vehicleRoutes;
        bestObjVal = currObjVal;
    }

    // Step and solve abstracted for different local search types
    public abstract boolean step() throws Exception;

    public abstract double solve() throws Exception;

    public void initRoutes(ArrayList<ArrayList<Integer>> initialRoutes) {
        assert (initialRoutes.size() <= vrp.numVehicles);
        for (int v = 0; v < initialRoutes.size(); v++) {
            vehicleRoutes[v] = new Route(vrp, v, customerNodes, initialRoutes.get(v));
        }
        for (int v = initialRoutes.size(); v < vrp.numVehicles; v++) {
            vehicleRoutes[v] = new Route(vrp, v, customerNodes, null);
        }
        // System.out.println("The initial routes are:");
        // for (Route r : vehicleRoutes) {
        //     r.printRoute();
        // }
        // System.out.println("");
        assert checker.check(vrp, vehicleRoutes);
    }

    public Proposed proposeRandomMove() {
        // System.out.println(String.format("Proposing random move\n"));

        while (true) {
            Node n1, n2;
            int choice = (int) Math.floor(generator.nextDouble() * 2);
            switch (choice) {
                case 0:
                    // Relocation case
                    n1 = customerNodes[(int) Math.floor(generator.nextDouble() * (customerNodes.length-1))+1];
                    n2 = customerNodes[(int) Math.floor(generator.nextDouble() * (customerNodes.length-1))+1];
                    if (n1 == n2) break;

                    if (checkRelocationFeasibility(n2.vehicle, n1)) {
                        double relocateCost = relocationCost(n1, n2);
                        return new Proposed(n1, n2, 0, relocateCost);
                    }
                    break;
                case 1:
                    // Swap case
                    n1 = customerNodes[(int) Math.floor(generator.nextDouble() * (customerNodes.length-1))+1];
                    n2 = customerNodes[(int) Math.floor(generator.nextDouble() * (customerNodes.length-1))+1];
                    if (n1 == n2) break;

                    if (checkSwappingFeasibility(n1.vehicle, n2.vehicle, n1, n2)) {
                        double cost;
                        // Neighbors cases
                        if (n1.next == n2) {
                            // !! Sets order to n1 -> n2 for all calls !!
                            cost = swappingNeighborsCost(n1, n2);
                            return new Proposed(n1, n2, 1, cost);
                        } else if (n2.next == n1) {
                            cost = swappingNeighborsCost(n2, n1);
                            return new Proposed(n2, n1, 1, cost);
                        } else {
                            // Standard case
                            cost = swappingCost(n1, n2);
                            return new Proposed(n1, n2, 1, cost);
                        }
                    }
                    break;
                default:
                    System.out.println("Default case reached.");
                    break;
            }
        }
    }

    public ArrayList<ArrayList<Integer>> initSolution() {
        ArrayList<ArrayList<Integer>> initial;
        // System.out.printf("Trying to find initial value\n", choice);

        initial = this.savings.run();
        if (initial != null) {
            System.out.println("Using savings.");
            return initial;
        }

        initial = this.sweep.run();
        if (initial != null) {
            System.out.println("Using sweep.");
            return initial;
        }

        initial = this.cp.run();
        if (initial != null) {
            cp.forbid(initial);
            System.out.println("Using cp.");
            return initial;
        }

        return null;
    }

    public double euclideanDistance(Node c1, Node c2) {
        double diffX = vrp.xCoordOfCustomer[c1.customer] - vrp.xCoordOfCustomer[c2.customer];
        double diffY = vrp.yCoordOfCustomer[c1.customer] - vrp.yCoordOfCustomer[c2.customer];
        return Math.sqrt(diffX * diffX + diffY * diffY);
    }

    public Node[] randomCustomerOrdering(Node[] nodes) {
        Node[] ordering = nodes.clone();
        int currIndex = nodes.length - 1;
        while (currIndex != 0) {
            int randomIndex = (int) Math.floor(generator.nextDouble() * currIndex);
            Node old = ordering[randomIndex];
            ordering[randomIndex] = ordering[currIndex];
            ordering[currIndex] = old;
            currIndex--;
        }
        return ordering;
    }

    // ==================
    // RELOCATION MOVES
    // ==================

    public boolean checkRelocationFeasibility(int vehicle, Node n) {
        return (vehicle == n.vehicle || vehicleRoutes[vehicle].demand +
                vrp.demandOfCustomer[n.customer] < vrp.vehicleCapacity);
    }

    public double relocateAddedDistance(Node n, Node newLocPrev) {
        return euclideanDistance(newLocPrev, n) +
                euclideanDistance(n, newLocPrev.next) -
                euclideanDistance(newLocPrev, newLocPrev.next);
    }

    public double relocateRemovedDistance(Node n) {
        return euclideanDistance(n.prev, n) +
                euclideanDistance(n, n.next) -
                euclideanDistance(n.prev, n.next);
    }

    public double relocationCost(Node node, Node newLocPrev) {
        return relocateAddedDistance(node, newLocPrev) - relocateRemovedDistance(node);
    }

    // Returns the difference in objective value as a result
    public double relocate(Node node, Node newLocPrev) throws Exception {
        // System.out.println("Relocation -----");
        // System.out.println(String.format("Customer %d, vehicle %d -> Prev %d, vehicle %d",
        //         node.customer, node.vehicle, newLocPrev.customer, newLocPrev.vehicle));
        Route r1 = vehicleRoutes[node.vehicle];
        double removedDist = relocateRemovedDistance(node);
        r1.remove(node, removedDist);
        Route r2 = vehicleRoutes[newLocPrev.vehicle];
        double addedDist = relocateAddedDistance(node, newLocPrev);
        r2.add(node, newLocPrev, relocateAddedDistance(node, newLocPrev));
        // System.out.println("----------------");
        return addedDist - removedDist;
    }

    // ==================
    // SWAPPING MOVES
    // ==================

    public boolean checkSwappingFeasibility(int v1, int v2, Node n1, Node n2) {
        return (v1 == v2 || ((vehicleRoutes[v1].demand + vrp.demandOfCustomer[n2.customer] -
                vrp.demandOfCustomer[n1.customer] < vrp.vehicleCapacity) && (vehicleRoutes[v2].demand +
                vrp.demandOfCustomer[n1.customer] - vrp.demandOfCustomer[n2.customer] < vrp.vehicleCapacity)));
    }

    public double swappingCost(Node n1, Node n2) {
        double old_n1 = euclideanDistance(n1.prev, n1) + euclideanDistance(n1, n1.next);
        double old_n2 = euclideanDistance(n2.prev, n2) + euclideanDistance(n2, n2.next);
        double new_n1 = euclideanDistance(n2.prev, n1) + euclideanDistance(n1, n2.next);
        double new_n2 = euclideanDistance(n1.prev, n2) + euclideanDistance(n2, n1.next);
        return new_n1 + new_n2 - old_n1 - old_n2;
    }

    // Assumes order is n1 -> n2
    public double swappingNeighborsCost(Node n1, Node n2) {
        double old_dist = euclideanDistance(n1.prev, n1) + euclideanDistance(n2, n2.next);
        double new_dist = euclideanDistance(n1.prev, n2) + euclideanDistance(n1, n2.next);
        return new_dist - old_dist;
    }

    public double swap(Node n1, Node n2) throws Exception {
        Route r1 = vehicleRoutes[n1.vehicle];
        // Neighboring nodes case
        if (n1.next == n2) {
            // !! Sets order to n1 -> n2 for all calls !!
            double cost = swappingNeighborsCost(n1, n2);
            r1.swapNeighbors(n1, n2, cost);
            return cost;
        } else if (n2.next == n1) {
            // !! Sets order to n2 -> n1 for all calls !!
            double cost = swappingNeighborsCost(n2, n1);
            r1.swapNeighbors(n2, n1, cost);
            return cost;
        } else {
            // Standard case
            Route r2 = vehicleRoutes[n2.vehicle];

            double cost = swappingCost(n1, n2);

            Node n1_prev = n1.prev;
            Node n2_prev = n2.prev;

            r1.remove(n1, relocateRemovedDistance(n1));
            r2.remove(n2, relocateRemovedDistance(n2));

            r2.add(n1, n2_prev, relocateAddedDistance(n1, n2_prev));
            r1.add(n2, n1_prev, relocateAddedDistance(n2, n1_prev));
            return cost;
        }
    }

    // ==================
    // UTILS
    // ==================

    public void printInitialRoutes(ArrayList<ArrayList<Integer>> initialRoutes) {
        System.out.println("The initial solution is:");
        for (int i = 0; i < initialRoutes.size(); i++) {
            for (int j = 0; j < initialRoutes.get(i).size(); j++) {
                System.out.print(String.format("%d ", initialRoutes.get(i).get(j)));
            }
            System.out.print("\n");
        }
    }

    public void initialRoutesToFile(ArrayList<ArrayList<Integer>> initialRoutes) throws FileNotFoundException {
        PrintStream out = new PrintStream(new FileOutputStream("solution/initial.sol"));
        String solution = String.format("-1 0", bestObjVal);
        for (int i = 0; i < initialRoutes.size(); i++) {
            solution += "\n0 ";
            for (int j = 0; j < initialRoutes.get(i).size(); j++) {
                solution += String.format("%d ", initialRoutes.get(i).get(j));
            }
            solution += "0";
        }
        out.print(solution);
        out.flush();
        out.close();
    }

    public String bestSolutionToString() {
        String solution = "";
        for (Route r : bestRoutes) {
            solution += "0 ";
            Node curr = r.routeCycle.depot.next;
            while (curr != r.routeCycle.depot) {
                solution += String.format("%d ", curr.customer);
                curr = curr.next;
            }
            solution += "0 ";
        }
        return solution;
    }

    public void bestSolutionToFile(String filename) throws FileNotFoundException {
        PrintStream out = new PrintStream(new FileOutputStream("solutions/" + filename + ".sol"));
        String solution = String.format("%.2f 0", bestObjVal);
        for (Route r : bestRoutes) {
            solution += "\n0 ";
            Node curr = r.routeCycle.depot.next;
            while (curr != r.routeCycle.depot) {
                solution += String.format("%d ", curr.customer);
                curr = curr.next;
            }
            solution += "0";
        }
        out.print(solution);
        out.flush();
        out.close();
    }

}

class Proposed {
    Node n1, n2;
    int move; // 0 for relocation, 1 for swap
    double delta;

    public Proposed(Node n1, Node n2, int move, double delta) {
        this.n1 = n1;
        this.n2 = n2;
        this.move = move;
        this.delta = delta;
    };
}
