package solver.initial.randomfeasible;

import solver.VRPInstance;

import ilog.cp.*;
import ilog.concert.*;

import java.util.ArrayList;

public class CPSolutionFinder {
  VRPInstance vrp;
  IloCP cp;
  
  /**
   * Constructor + parser
   */
  public CPSolutionFinder(VRPInstance vrp) {
    this.vrp = vrp;
  }

  /**
   * Build and run CP
   */
  public ArrayList<ArrayList<Integer>> run() {
    try {
      cp = new IloCP();
      cp.setParameter(IloCP.IntParam.Workers, 1);
      cp.setParameter(IloCP.DoubleParam.TimeLimit, 30);

      // Construct data array
      IloIntVar[][] data = new IloIntVar[this.vrp.numVehicles][this.vrp.numCustomers-1];
      for (int v = 0; v < this.vrp.numVehicles; v++) {
        for (int c = 0; c < this.vrp.numCustomers-1; c++) {
          data[v][c] = cp.intVar(0, 1);
        }
      }

      // Each customer should be served by exactly one vehicle
      for (int c = 0; c < this.vrp.numCustomers-1; c++) {
        IloIntExpr[] serving = new IloIntExpr[this.vrp.numVehicles];
        for (int v = 0; v < this.vrp.numVehicles; v++) {
          serving[v] = data[v][c];
        }
        cp.add(cp.eq(cp.count(serving, 1), 1));
      }

      // Remove first entry from demand
      int[] demandOfCustomer = new int[this.vrp.numCustomers-1];
      for (int c = 0; c < this.vrp.numCustomers-1; c++) {
        demandOfCustomer[c] = this.vrp.demandOfCustomer[c+1];
      }

      // Sum of vehicle demands must be less than the max
      for (int v = 0; v < this.vrp.numVehicles; v++) {
        IloIntExpr[] served = new IloIntExpr[this.vrp.numCustomers-1];
        for (int c = 0; c < this.vrp.numCustomers-1; c++) {
          served[c] = data[v][c];
        }
        cp.add(cp.le(cp.prod(served, demandOfCustomer), this.vrp.vehicleCapacity));
      }

      // Solve and return output.
      cp.solve();
      ArrayList<ArrayList<Integer>> res = new ArrayList<>();
      for (int v = 0; v < this.vrp.numVehicles; v++) {
        ArrayList<Integer> curr = new ArrayList<>();
        for (int c = 0; c < this.vrp.numCustomers-1; c++) {
          if (cp.getValue(data[v][c]) == 1) {
            curr.add(c);
          }
        }
        res.add(curr);
      }
      return res;
    } catch (IloException e) {
      System.out.println("Error: " + e);
      return null;
    }
  }
}
