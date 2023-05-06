package solver.ls;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import java.lang.Math;
import java.util.ArrayList;

// IP Approach
public class IPModel {
    IloCplex cp;
    VRPInstance vrp;
    IloNumVar[][][] vehicleArcChoice;
    boolean optimal = false;

    // Constructor
    public IPModel(VRPInstance vrp) throws IloException {
        this.cp = new IloCplex();
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
    private int[][][] getInformation() throws IloException {
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
    public double solve() throws IloException {
        optimal = true;

        // Keep solving until we get no subtours
        while (true) {
            // Solve...
            cp.solve();
            
            // Add constraints for all extra loops.
            boolean loops = false;
            int[][][] M = getInformation();
            for (int v = 0; v < vrp.numVehicles; v++) {
                boolean[] visited = new boolean[vrp.numCustomers];

                // 1) DFS starting at the depot to see which loop is acceptable.
                int curr = 0;
                while (true) {
                    // Mark as visited.
                    visited[curr] = true;

                    // Check if we can go to the depot; if so, we are done.
                    if (M[v][curr][0] == 1) {
                        break;
                    }

                    // Otherwise, go through the other locations and see what our next hop is.
                    boolean found = false;
                    for (int dest = 1; dest < vrp.numCustomers; dest++) {
                        if (M[v][curr][dest] == 1) {
                            curr = dest;
                            break;
                        }
                    }
                    if (!found) break; // Try the next src if there are no edges coming from this one (unsure if this is possible)
                }
                
                // DFS starting at every possible customer to detect how many extra loops we have.
                for (int src = 1; src < vrp.numCustomers; src++) {
                    if (visited[src]) continue;

                    // Keep track of this subtour
                    ArrayList<Integer> path = new ArrayList<>();
                    ArrayList<IloNumExpr> subtour = new ArrayList<>();
                    curr = src;
                    while (true) {
                        // Mark as visited.
                        visited[curr] = true;
                        path.add(curr);

                        // Check if we can go to the start of the loop; if so, we are done.
                        if (M[v][curr][src] == 1) {
                            subtour.add(vehicleArcChoice[v][curr][src]);
                            break;
                        }

                        // Otherwise, go through the other locations and see what our next hop is.
                        boolean found = false;
                        for (int dest = 1; dest < vrp.numCustomers; dest++) {
                            if (M[v][curr][dest] == 1) {
                                subtour.add(vehicleArcChoice[v][curr][dest]);
                                curr = dest;
                                found = true;
                                break;
                            }
                        }
                        if (!found) break; // Try the next src if there are no edges coming from this one
                    }

                    // We need to enforce that all of these edges are never taken for any vehicle.
                    if (subtour.size() > 0) {
                        System.out.println("WE ARE REMOVING THE FOLLOWING TOUR:");
                        System.out.println(path);
                        cp.addLe(cp.sum(subtour.toArray(new IloNumExpr[subtour.size()])), subtour.size() - 1);
                        loops = true;
                    }
                }
            }
            if (!loops) return cp.getObjValue();
        }
    }

    //  Extracts the solution into the desired format
    public String printSolution() throws IloException {
        // Print out the objective value and whether or not it was optimal
        String s = String.format("%.2f %d\n", cp.getObjValue(), optimal ? 1 : 0);

        // Go through each vehicle and see what it's tour was
        int[][][] M = getInformation();
        for (int v = 0; v < vrp.numVehicles; v++) {
            String row = "0 ";
            int curr = 0;

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
            
            // Once we are back to the depot, add this row and do the next vehicle.
            s += row + "\n";
        }
        return s;
    }
}
