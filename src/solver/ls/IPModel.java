package solver.ls;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import java.lang.Math;


public class IPModel {

    IloCplex cp;
    VRPInstance vrp;
    IloNumVar[][][] vehicleArcChoice;

    public IPModel(VRPInstance vrp) throws IloException {
        this.cp = new IloCplex();
        this.vrp = vrp;
        this.initIPModel();
    }

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

        // Symmetry breaking constraint
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

        // Subtour elimination constraint
//        for (int v = 0; v < vrp.numVehicles; v++) {
//            Math.
//        }

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

    public double solve() throws IloException {
        cp.solve();

        for (int v = 0; v < vrp.numVehicles; v++) {
            for (int i = 0; i < vrp.numCustomers; i++) {
                for (int j = 0; j < vrp.numCustomers; j++) {
                    System.out.printf("v: %d, i: %d, j: %d : %f\n", v, i, j, cp.getValue(vehicleArcChoice[v][i][j]));
                }
            }
        }

        return cp.getObjValue();
    }

}
