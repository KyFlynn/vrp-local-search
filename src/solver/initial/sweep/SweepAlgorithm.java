package solver.initial.sweep;

import solver.VRPInstance;

import java.lang.Comparable;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

// Edge class 
class Customer implements Comparable<Customer> {
  int id;
  double x, y, theta;

  public Customer(int id, double x, double y) {
    this.id = id;
    this.x = x;
    this.y = y;
    this.theta = Math.atan2(y, x) + Math.PI;
  }

  @Override
	public int compareTo(Customer e) {
		return this.theta < e.theta ? 1 : (this.theta > e.theta ? -1 : 0);
	}
}

// Implement sweep algorithm
public class SweepAlgorithm {
    VRPInstance vrp;

    // Constructor
    public SweepAlgorithm(VRPInstance vrp) {
        this.vrp = vrp;
    }

    // Run
    public ArrayList<ArrayList<Integer>> run() {
        // Make customers and sort by radius
        ArrayList<Customer> customers = new ArrayList<>();
        for (int c = 1; c < this.vrp.numCustomers; c++) {
          Customer customer = new Customer(c, this.vrp.xCoordOfCustomer[c], this.vrp.yCoordOfCustomer[c]);
          customers.add(customer);
        }
        Collections.sort(customers);

        // Sweep, baby
        ArrayList<ArrayList<Integer>> res = new ArrayList<>();
        ArrayList<Integer> curr = new ArrayList<>();
        int curr_capacity = 0;
        for (Customer c : customers) {
            if (curr_capacity + this.vrp.demandOfCustomer[c.id] > this.vrp.vehicleCapacity) {
              res.add(curr);
              curr = new ArrayList<>();
              curr_capacity = 0;
            }
            curr.add(c.id);
            curr_capacity += this.vrp.demandOfCustomer[c.id];
        }
        return res;
    }
}