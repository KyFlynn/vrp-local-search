package solver.complete;

import solver.util.Timer;
import solver.VRPInstance;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;

// IP Approach
public class IPModel {
    IloCplex cp;
    VRPInstance vrp;
    IloNumVar[][][] vehicleArcChoice;
    boolean feasible = false;

    // Constructor
    public IPModel(VRPInstance vrp) throws IloException, FileNotFoundException {
        this.cp = new IloCplex();
//        OutputStream out = new FileOutputStream("output.txt");
//        this.cp.setOut(out);
        this.vrp = vrp;
        this.initIPModel();
    }

    // Initialize model
    public void initIPModel() throws IloException {
        // Create decision variable
        vehicleArcChoice = new IloNumVar[vrp.numVehicles][vrp.numCustomers][vrp.numCustomers];
        for (int v = 0; v < vrp.numVehicles; v++) {
            for (int i = 0; i < vrp.numCustomers; i++) {
                for (int j = 0; j < vrp.numCustomers; j++) {
                    vehicleArcChoice[v][i][j] = cp.numVar(0, 1, IloNumVarType.Int);
                }
            }
        }

        // Don't use edges to self except depot (index 0)
        for (int ij = 1; ij < vrp.numCustomers; ij++) {
            for (int v = 0; v < vrp.numVehicles; v++) {
                cp.addEq(vehicleArcChoice[v][ij][ij], 0);
            }
        }

        // Edges are undirected
        // for (int i = 0; i < vrp.numCustomers; i++) {
        //     for (int j = i+1; j < vrp.numCustomers; j++) {
        //         for (int v = 0; v < vrp.numVehicles; v++) {
        //             cp.addEq(vehicleArcChoice[v][i][j], vehicleArcChoice[v][j][i]);
        //         }
        //     }
        // }

        // Each edge should only be taken by one vehicle
        for (int i = 0; i < vrp.numCustomers; i++) {
            for (int j = i+1; j < vrp.numCustomers; j++) {
                IloNumExpr[] vehicleSlice = new IloNumExpr[vrp.numVehicles];
                for (int v = 0; v < vrp.numVehicles; v++) {
                    vehicleSlice[v] = vehicleArcChoice[v][i][j];
                }
                cp.addLe(cp.sum(vehicleSlice), 1);
            }
        }

        // Each customer is served (exit)
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

        // Each customer is served (entry)
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
        cp.addMinimize(cp.sum(totalDistance));

        // Save model to file for visual check -> ".Lp" extension required
        cp.exportModel("Model.Lp");
    }

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

    // Solve - this will loop until there are no subtours
    public double solve() throws Exception {
        feasible = true;
        while (true) {
            feasible = cp.solve();
            boolean subtoursExist = false;
            int[][][] M = getVariableValues();
//            System.out.println(Arrays.deepToString(M));
            boolean[] visited = new boolean[vrp.numCustomers];
            for (int v = 0; v < vrp.numVehicles; v++) {
//                System.out.println(String.format("Analyzing Vehicle: %d", v));

                // 1) DFS starting at the depot to see which customers are in a valid tour.

                // Keep track of the tour for printing.
                ArrayList<Integer> tour = new ArrayList<>();
                // Start at depot.
                int curr = 0;
                tour.add(curr);
                visited[curr] = true;
                // Check if an exit exists from the depot.
                for (int i = 1; i < vrp.numCustomers; i++) {
                    if (M[v][curr][i] == 1) {
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
                        if (M[v][curr][0] == 1) {
                            break;
                        }

                        // Otherwise, travel through the arc
                        boolean found = false;
                        for (int dest = 1; dest < vrp.numCustomers; dest++) {
                            if (M[v][curr][dest] == 1) {
                                curr = dest;
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
//                            System.out.println(Arrays.deepToString(M));
                            throw new Exception("Path is not a tour, model was implemented incorrectly.");
                        }
                    }
                }
//                System.out.println("TOUR FOUND:");
//                System.out.println(tour);

                // 2) DFS through all other arcs for this vehicle to see which customers are in a subtour.

                for (int i = 1; i < vrp.numCustomers; i++) {
                    // Customer already included in a discovered tour/subtour
                    if (visited[i]) continue;
                    for (int j = 1; j < vrp.numCustomers; j++) {
                        // Customer already included in a discovered tour/subtour
                        if (visited[j]) continue;

                        // Subtour arc found
                        if (M[v][i][j] == 1) {
                            // Keep track of this subtour
                            ArrayList<Integer> subtourCustomers = new ArrayList<>();
                            ArrayList<IloNumExpr> subtour = new ArrayList<>();
                            // Source is i
                            int src = i;
                            // Mark source as visited and add to subtour.
                            visited[src] = true;
                            subtourCustomers.add(src);
                            // Travel through the first arc found, then DFS.
                            curr = j;
                            subtour.add(vehicleArcChoice[v][src][curr]);
                            while (true) {
                                // Mark as visited.
                                visited[curr] = true;
                                subtourCustomers.add(curr);

                                // Check if we go to the source of the subtour next; if so we are done.
                                if (M[v][curr][src] == 1) {
                                    subtour.add(vehicleArcChoice[v][curr][src]);
                                    break;
                                }

                                // Otherwise, go through the next arc in the subtour.
                                boolean found = false;
                                for (int dest = 1; dest < vrp.numCustomers; dest++) {
                                    if (M[v][curr][dest] == 1) {
                                        curr = dest;
                                        subtour.add(vehicleArcChoice[v][curr][dest]);
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
//                                    System.out.println(Arrays.deepToString(M));
                                    throw new Exception("Path is not a subtour, model was implemented incorrectly.");
                                }
                            }
                            // We need to enforce that all of these edges are never taken for any vehicle.
                            if (subtour.size() > 0) {
//                                System.out.println("SUBTOUR FOUND:");
//                                System.out.println(subtourCustomers);
                                cp.addLe(cp.sum(subtour.toArray(new IloNumExpr[subtour.size()])), subtour.size() - 1);
                                subtoursExist = true;
                            }
                        }
                    }
                }
            }
            if (!subtoursExist) return cp.getObjValue();
        }
    }

    //  Extracts the solution into the desired format
    public String printSolution() throws IloException {
        // Print out the objective value and whether it was optimal
        String s = String.format("%.2f %d\n", cp.getObjValue(), feasible ? 1 : 0);

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
                row += "0";
            }

            // Once we are back to the depot, add this row and do the next vehicle.
            s += row + "\n";
        }
        return s;
    }
}
