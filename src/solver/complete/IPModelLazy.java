package solver.complete;

import ilog.concert.*;
import ilog.cplex.*;
import solver.VRPInstance;
import solver.incomplete.DisturbedBestImprovement;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

public class IPModelLazy {

    /** Epsilon used for lazy constraint checks. */
    private static double EPS = 1e-6;

    IloCplex cp;
    VRPInstance vrp;
    IloNumVar[][][] vehicleArcChoice;

    public IPModelLazy(VRPInstance vrp) throws Exception {
        this.cp = new IloCplex();
        this.vrp = vrp;
        this.initIPModel();
    }

    public double solve() throws IloException {
        // Now we get to setting up the generic callback.
        // We instantiate a FacilityCallback and set the contextMask parameter.
        VRPCallback cb = new VRPCallback(vehicleArcChoice, vrp);
        long contextMask = 0;
        contextMask |= IloCplex.Callback.Context.Id.Candidate;
        cp.use(cb, contextMask);

        if ( !cp.solve() )
            throw new RuntimeException("No feasible solution found");

        System.out.println("Solution status:                   " +
                cp.getStatus());
        System.out.println("Active user cuts/lazy constraints: " +
                cp.getNcuts(IloCplex.CutType.User));
        double objVal = cp.getObjValue();
        System.out.println("Optimal value:                     " +
                objVal);
        return objVal;
    }


    // Initialize model
    public void initIPModel() throws Exception {
        // Create decision variable
        vehicleArcChoice = new IloNumVar[vrp.numVehicles][vrp.numCustomers][vrp.numCustomers];
        for (int v = 0; v < vrp.numVehicles; v++) {
            for (int i = 0; i < vrp.numCustomers; i++) {
                for (int j = 0; j < vrp.numCustomers; j++) {
                    vehicleArcChoice[v][i][j] = cp.numVar(0, 1, IloNumVarType.Int, String.format("V%dC%d,%d", v, i, j));
                }
            }
        }

        // Don't use edges to self except depot (index 0)
        for (int ij = 1; ij < vrp.numCustomers; ij++) {
            for (int v = 0; v < vrp.numVehicles; v++) {
                cp.addEq(vehicleArcChoice[v][ij][ij], 0);
            }
        }

        // Each edge (undirected) should only be taken by one vehicle
        for (int i = 0; i < vrp.numCustomers; i++) {
            for (int j = i+1; j < vrp.numCustomers; j++) {
                IloNumExpr[] vehicleSlice = new IloNumExpr[2 * vrp.numVehicles];
                for (int v = 0; v < vrp.numVehicles; v++) {
                    // Both directions
                    vehicleSlice[2 * v] = vehicleArcChoice[v][i][j];
                    vehicleSlice[2 * v + 1] = vehicleArcChoice[v][j][i];
                }
                cp.addLe(cp.sum(vehicleSlice), 1);
            }
        }

        // Each customer is served (entry)
        for (int i = 0; i < vrp.numCustomers; i++) {
            IloNumExpr[] vehicleCustomerSlice = new IloNumExpr[vrp.numVehicles * vrp.numCustomers];
            for (int v = 0; v < vrp.numVehicles; v++) {
                for (int j = 0; j < vrp.numCustomers; j++) {
                    vehicleCustomerSlice[v * vrp.numCustomers + j] = vehicleArcChoice[v][i][j];
                }
            }
            if (i == 0) {
                cp.addEq(cp.sum(vehicleCustomerSlice), vrp.numVehicles);
            } else {
                cp.addEq(cp.sum(vehicleCustomerSlice), 1);
            }
        }

        // Each customer is served (exit)
        for (int j = 0; j < vrp.numCustomers; j++) {
            IloNumExpr[] vehicleCustomerSlice = new IloNumExpr[vrp.numVehicles * vrp.numCustomers];
            for (int v = 0; v < vrp.numVehicles; v++) {
                for (int i = 0; i < vrp.numCustomers; i++) {
                    vehicleCustomerSlice[v * vrp.numCustomers + i] = vehicleArcChoice[v][i][j];
                }
            }
            if (j == 0) {
                cp.addEq(cp.sum(vehicleCustomerSlice), vrp.numVehicles);
            } else {
                cp.addEq(cp.sum(vehicleCustomerSlice), 1);
            }
        }

        // Each vehicle has a tour
        for (int c = 0; c < vrp.numCustomers; c++) {
            for (int v = 0; v < vrp.numVehicles; v++) {
                IloNumExpr[] iSlice = new IloNumExpr[vrp.numCustomers];
                for (int i = 0; i < vrp.numCustomers; i++) {
                    iSlice[i] = vehicleArcChoice[v][i][c];
                }
                IloNumExpr[] jSlice = new IloNumExpr[vrp.numCustomers];
                for (int j = 0; j < vrp.numCustomers; j++) {
                    jSlice[j] = vehicleArcChoice[v][c][j];
                }
                cp.addEq(cp.diff(cp.sum(iSlice), cp.sum(jSlice)), 0);
            }
        }

        // Capacity constraint
        for (int v = 0; v < vrp.numVehicles; v++) {
            IloNumExpr[] outSlice = new IloNumExpr[vrp.numCustomers];
            for (int i = 0; i < vrp.numCustomers; i++) {
                int demand = vrp.demandOfCustomer[i];
                IloNumExpr[] inSlice = new IloNumExpr[vrp.numCustomers];
                for (int j = 0; j < vrp.numCustomers; j++) {
                    inSlice[j] = vehicleArcChoice[v][i][j];
                }
                outSlice[i] = cp.prod(cp.sum(inSlice), demand);
            }
            cp.addLe(cp.sum(outSlice), vrp.vehicleCapacity);
        }

        // Distances as expression of all variables
        IloNumExpr[] distances = getDistances();

        // Restrict objective value to smaller than some feasible initial solution:
        DisturbedBestImprovement solver = new DisturbedBestImprovement(vrp);
        solver.runtime = 30;
        solver.solve();
        cp.addLe(cp.sum(distances), solver.bestObjVal);

        // Objective function
        cp.addMinimize(cp.sum(distances));

        // Save model to file for visual check -> ".Lp" extension required
        // cp.exportModel("Model.Lp");
    }

    // Expression of distances using variable values
    private IloNumExpr[] getDistances() throws IloException {
        // Objective function
        IloNumExpr[] totalDistance = new IloNumExpr[vrp.numVehicles * vrp.numCustomers * vrp.numCustomers];
        int idx = 0;
        for (int i = 0; i < vrp.numCustomers; i++) {
            for (int j = 0; j < vrp.numCustomers; j++) {
                double distance = Math.sqrt(
                        Math.pow(vrp.xCoordOfCustomer[i] - vrp.xCoordOfCustomer[j], 2) +
                                Math.pow(vrp.yCoordOfCustomer[i] - vrp.yCoordOfCustomer[j], 2)
                );
                for (int v = 0; v < vrp.numVehicles; v++) {
                    totalDistance[idx] = cp.prod(vehicleArcChoice[v][i][j], distance);
                    idx++;
                }
            }
        }
        return totalDistance;
    }

    /**
     * This is the class implementing the generic callback interface to add constraints lazily.
     */
    private static class VRPCallback implements IloCplex.Callback.Function {
        private final IloNumVar[][][] vehicleArcChoice;
        private final VRPInstance vrp;
        public VRPCallback(IloNumVar[][][] vehicleArcChoice,
                                VRPInstance vrp)
        {
            this.vehicleArcChoice = vehicleArcChoice;
            this.vrp = vrp;
        }

        /**
         * Lazy constraint callback to enforce the subtour constraints dynamically.
         *
         * If used then the callback is invoked for every integer feasible
         * solution CPLEX finds. For each location j it checks whether
         * constraint
         *    sum(c in C) supply[c][j] <= (|C| - 1) * opened[j]
         * is satisfied. If not then it adds the violated constraint as lazy
         * constraint.
         */
        private void lazySubtourConstraint (IloCplex.Callback.Context context) throws Exception {
            final IloCplexModeler m = context.getCplex();
            if ( !context.isCandidatePoint() )
                throw new IloException("Unbounded solution");

            // 1) DFS starting at the depot to see which customers are in a valid tour.
            boolean[] visited = new boolean[vrp.numCustomers];
            for (int v = 0; v < vrp.numVehicles; v++) {
                // Keep track of the tour for printing.
                ArrayList<Integer> tour = new ArrayList<>();
                // Start at depot.
                int curr = 0;
                tour.add(curr);
                visited[curr] = true;
                // Check if an exit exists from the depot.
                for (int i = 1; i < vrp.numCustomers; i++) {
                    if (Math.abs(context.getCandidatePoint(vehicleArcChoice[v][curr][i]) - 1) < EPS) {
                        curr = i;
                        break;
                    }
                }
                // If a tour other than depot self tour exists, begin DFS on it.
                if (curr > 0) {
                    while (true) {
                        // Mark as visited.
                        visited[curr] = true;
                        tour.add(curr);

                        // Check if we go to the depot next; if so we are done.
                        if (Math.abs(context.getCandidatePoint(vehicleArcChoice[v][curr][0]) - 1) < EPS) {
                            break;
                        }

                        // Otherwise, travel through the arc
                        boolean found = false;
                        for (int dest = 1; dest < vrp.numCustomers; dest++) {
                            if (Math.abs(context.getCandidatePoint(vehicleArcChoice[v][curr][dest]) - 1) < EPS) {
                                curr = dest;
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            System.out.println(String.format("Issue with tour in vehicle %d found:", v));
                            for (int i = 0; i < tour.size(); i++) {
                                System.out.print(String.format("%d ", tour.get(i)));
                            }
                            System.out.println("");
                            printCandidateValues(context);
                            throw new Exception("Path is not a tour, model was implemented incorrectly.");
                        }
                    }
                }
                // System.out.println("Tour found:");
                // for (int i = 0; i < tour.size(); i++) {
                //     System.out.print(String.format("%d ", tour.get(i)));
                // }
                // System.out.println("");
            }

            // 2) DFS through all other arcs for this vehicle to see which customers are in a subtour.
            for (int v = 0; v < vrp.numVehicles; v++) {
                for (int i = 1; i < vrp.numCustomers; i++) {
                    // Customer already included in a discovered tour/subtour
                    if (visited[i]) continue;
                    for (int j = 1; j < vrp.numCustomers; j++) {
                        // Customer already included in a discovered tour/subtour
                        if (visited[j]) continue;
                        // Subtour arc found
                        if (Math.abs(context.getCandidatePoint(vehicleArcChoice[v][i][j]) - 1) < EPS) {
                            // Keep track of this subtour
                            ArrayList<Integer> subtourCustomers = new ArrayList<>();
                            // Source is i
                            int src = i;
                            // Mark source as visited and add to subtour.
                            visited[src] = true;
                            subtourCustomers.add(src);
                            // Travel through the first arc found, then DFS.
                            int curr = j;
                            while (true) {
                                // Mark as visited.
                                visited[curr] = true;
                                subtourCustomers.add(curr);

                                // Check if we go to the source of the subtour next; if so we are done.
                                if (Math.abs(context.getCandidatePoint(vehicleArcChoice[v][curr][src]) - 1) < EPS) {
                                    break;
                                }

                                // Otherwise, go through the next arc in the subtour.
                                boolean found = false;
                                for (int dest = 1; dest < vrp.numCustomers; dest++) {
                                    if (Math.abs(context.getCandidatePoint(vehicleArcChoice[v][curr][dest]) - 1) < EPS) {
                                        curr = dest;
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    throw new Exception("Path is not a subtour, model was implemented incorrectly.");
                                }
                            }
                            // We need to enforce that all of these subtours are never taken for any vehicle.
                            if (subtourCustomers.size() > 0) {
                                // System.out.println("Subtour found:");
                                // for (int c = 0; c < subtourCustomers.size(); c++) {
                                //     System.out.print(String.format("%d ", subtourCustomers.get(c)));
                                // }
                                // System.out.println("");
                                IloLinearNumExpr subtourEdges = m.linearNumExpr();
                                for (int veh = 0; veh < vrp.numVehicles; veh++) {
                                    subtourEdges.addTerm(1,
                                            vehicleArcChoice[veh][subtourCustomers.get(subtourCustomers.size()-1)][subtourCustomers.get(0)]);
                                    subtourEdges.addTerm(1,
                                            vehicleArcChoice[veh][subtourCustomers.get(0)][subtourCustomers.get(subtourCustomers.size()-1)]);
                                    for (int c = 1; c < subtourCustomers.size(); c++) {
                                        subtourEdges.addTerm(1,
                                                vehicleArcChoice[veh][subtourCustomers.get(c-1)][subtourCustomers.get(c)]);
                                        subtourEdges.addTerm(1,
                                                vehicleArcChoice[veh][subtourCustomers.get(c)][subtourCustomers.get(c-1)]);
                                    }
                                }
                                // Restrict objective value to larger values
                                // context.rejectCandidate(m.ge(m.sum(getDistances(m)), context.getCandidateObjective()));
                                // Reject candidate with subtour:
                                context.rejectCandidate(m.le(subtourEdges, subtourCustomers.size() - 1));
                                // System.out.println("Adding lazy capacity constraint " + subtourEdges +
                                //         " <= " + (subtourCustomers.size() - 1));
                            }
                        }
                    }
                }
            }
        }

        // Get the vehicle route information into a nice adjacency chart.
        private void printCandidateValues(IloCplex.Callback.Context context) throws IloException {
            int[][][] M = new int[vrp.numVehicles][vrp.numCustomers][vrp.numCustomers];
            for (int v = 0; v < vrp.numVehicles; v++) {
                for (int i = 0; i < vrp.numCustomers; i++) {
                    for (int j = 0; j < vrp.numCustomers; j++) {
                        M[v][i][j] = (int) context.getCandidatePoint(vehicleArcChoice[v][i][j]);
                    }
                }
            }
            System.out.println(Arrays.deepToString(M));;
        }

        // Expression of distances using variable values
        private IloNumExpr[] getDistances(IloCplexModeler m) throws IloException {
            // Objective function
            IloNumExpr[] totalDistance = new IloNumExpr[vrp.numVehicles * vrp.numCustomers * vrp.numCustomers];
            int idx = 0;
            for (int i = 0; i < vrp.numCustomers; i++) {
                for (int j = 0; j < vrp.numCustomers; j++) {
                    double distance = Math.sqrt(
                            Math.pow(vrp.xCoordOfCustomer[i] - vrp.xCoordOfCustomer[j], 2) +
                                    Math.pow(vrp.yCoordOfCustomer[i] - vrp.yCoordOfCustomer[j], 2)
                    );
                    for (int v = 0; v < vrp.numVehicles; v++) {
                        totalDistance[idx] = m.prod(vehicleArcChoice[v][i][j], distance);
                        idx++;
                    }
                }
            }
            return totalDistance;
        }

        /**
         * Implements the required invoke method.
         *
         * This is the method that we have to implement to fulfill the
         * generic callback contract. CPLEX will call this method during
         * the solution process at the places that we asked for.
         */
        @Override
        public void invoke (IloCplex.Callback.Context context) {
            if (context.inCandidate()) {
                try {
                    lazySubtourConstraint (context);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }


    }

    // ==================
    // UTILS
    // ==================

    // Get the vehicle route information into a nice adjacency chart.
    private int[][][] getVariableValues() throws IloException {
        // Let's grab the variables out bc they ain't shit
        int[][][] M = new int[vrp.numVehicles][vrp.numCustomers][vrp.numCustomers];
        for (int v = 0; v < vrp.numVehicles; v++) {
            for (int i = 0; i < vrp.numCustomers; i++) {
                for (int j = 0; j < vrp.numCustomers; j++) {
                    M[v][i][j] = (int) cp.getValue(vehicleArcChoice[v][i][j]);
                }
            }
        }
        return M;
    }

    //  Extracts the solution into the desired format
    public String solutionToString() throws IloException {
        String s = "";
        // Go through each vehicle and see what its tour was
        int[][][] M = getVariableValues();
        for (int v = 0; v < vrp.numVehicles; v++) {
            String row = "0 ";
            int curr = 0;
            // Check if an exit exists from the depot.
            for (int i = 1; i < vrp.numCustomers; i++) {
                if (M[v][curr][i] == 1) {
                    curr = i;
                    break;
                }
            }
            // If a non-depot exit exists
            if (curr > 0) {
                row += String.format("%d ", curr);
                // Until we get back to the depot...
                while (true) {
                    // Check if we can go to the depot; if so, we are done.
                    if (M[v][curr][0] == 1) {
                        row += "0";
                        break;
                    }

                    // Otherwise, go through the other locations and see what our next hop is.
                    for (int dest = 1; dest < vrp.numCustomers; dest++) {
                        if (M[v][curr][dest] == 1) {
                            curr = dest;
                            row += String.format("%d ", dest);
                            break;
                        }
                    }
                }
            } else {
                // Note: a case where no arc is chosen for the vehicle exists, so default to arc 0 - 0
                row += "0 ";
            }
            // Once we are back to the depot, add this row and do the next vehicle.
            s += row;
        }
        return s;
    }

    public void solutionToFile(String filename) throws FileNotFoundException, IloException {
        String s = String.format("%.2f %d\n", cp.getObjValue(), 1);
        int[][][] M = getVariableValues();
        for (int v = 0; v < vrp.numVehicles; v++) {
            String row = "0 ";
            int curr = 0;
            // Check if an exit exists from the depot.
            for (int i = 1; i < vrp.numCustomers; i++) {
                if (M[v][curr][i] == 1) {
                    curr = i;
                    break;
                }
            }
            // If a non-depot exit exists
            if (curr > 0) {
                row += String.format("%d ", curr);
                // Until we get back to the depot...
                while (true) {
                    // Check if we can go to the depot; if so, we are done.
                    if (M[v][curr][0] == 1) {
                        row += "0";
                        break;
                    }

                    // Otherwise, go through the other locations and see what our next hop is.
                    for (int dest = 1; dest < vrp.numCustomers; dest++) {
                        if (M[v][curr][dest] == 1) {
                            curr = dest;
                            row += String.format("%d ", dest);
                            break;
                        }
                    }
                }
            } else {
                // Note: a case where no arc is chosen for the vehicle exists, so default to arc 0 - 0
                row += "0";
            }
            // Once we are back to the depot, add this row and do the next vehicle.
            s += row + "\n";
        }
        PrintStream out = new PrintStream(new FileOutputStream("input/" + filename + ".sol"));
        out.print(s);
        out.flush();
        out.close();
    }

}
