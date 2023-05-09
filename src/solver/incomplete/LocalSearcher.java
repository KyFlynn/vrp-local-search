package solver.incomplete;

import solver.VRPInstance;
import solver.initial.savings.SavingsAlgorithm;
import solver.initial.sweep.SweepAlgorithm;
import solver.util.Node;
import solver.util.Route;
import solver.util.Timer;

import java.util.ArrayList;
import java.util.Random;


public abstract class LocalSearcher {

    VRPInstance vrp;
    int runtime;
    Random generator = new Random();  // Set seed if you want one.
    Node[] customerNodes;
    Route[] vehicleRoutes;
    double currObjVal;
    Route[] bestRoutes;
    double bestObjVal;

    public LocalSearcher(VRPInstance instance, int solveTime) {
        runtime = solveTime;
        vrp = instance;
        customerNodes =  new Node[vrp.numCustomers];
        for (int i = 0; i < vrp.numCustomers; i++) {
            customerNodes[i] = new Node(null, i, -1, null);
        }
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

    public double euclideanDistance(Node c1, Node c2) {
        double diffX = vrp.xCoordOfCustomer[c1.customer] - vrp.xCoordOfCustomer[c2.customer];
        double diffY = vrp.yCoordOfCustomer[c1.customer] - vrp.yCoordOfCustomer[c2.customer];
        return Math.sqrt(diffX * diffX + diffY * diffY);
    }

    // Assumes initialRoutes argument satisfied numVehicles constraint.
    public void initRoutes(ArrayList<ArrayList<Integer>>  initialRoutes) {
        assert (initialRoutes.size() <= vrp.numVehicles);
        for (int v = 0; v < initialRoutes.size(); v++) {
            vehicleRoutes[v] = new Route(vrp, v, customerNodes, initialRoutes.get(v));
        }
    }

    public ArrayList<ArrayList<Integer>> initSolution() {
        SavingsAlgorithm savings = new SavingsAlgorithm(vrp);
        ArrayList<ArrayList<Integer>> initial = savings.run();
        if (initial != null) {
            return initial;
        } else {
            SweepAlgorithm sweep = new SweepAlgorithm(vrp);
            initial = sweep.run();
            return initial;
        }
    }

    public double solve() throws Exception {
        Timer timer = new Timer();
        timer.start();
        while(timer.getTime() < runtime) {
            step();
            if (currObjVal < bestObjVal) {
                bestObjVal = currObjVal;
                bestRoutes = vehicleRoutes;
            }
        }
        return bestObjVal;
    }

    public abstract void step() throws Exception;

    public abstract void searchNeighborhood(Node n) throws Exception;

    public abstract Node chooseRelocationNode();

    public boolean checkRelocationFeasibility(int vehicle, Node n) {
        return (vehicleRoutes[vehicle].demand + vrp.demandOfCustomer[n.customer] < vrp.vehicleCapacity);
    }

    public double addedDistance(Node n, Node newLocPrev) {
        return euclideanDistance(newLocPrev, n) +
                euclideanDistance(n, newLocPrev.next) -
                euclideanDistance(newLocPrev, newLocPrev.next);
    }

    public double removedDistance(Node n) {
        return euclideanDistance(n, n.next) +
                euclideanDistance(n.prev, n) -
                euclideanDistance(n.prev, n.next);
    }

    public double relocationScore(Node node, Node newLocPrev) {
        return addedDistance(node, newLocPrev) - removedDistance(node);
    }

    public void relocate(Node node, Node newLocPrev) throws Exception {
        Route r1 = vehicleRoutes[node.vehicle];
        r1.remove(node, removedDistance(node));
        Route r2 = vehicleRoutes[newLocPrev.vehicle];
        r2.add(node, newLocPrev, newLocPrev.vehicle, addedDistance(node, newLocPrev));
    }

    // TODO: Add swapping neighborhoods on top of relocation (relocation may be infeasible while swapping feasible)
    
}
