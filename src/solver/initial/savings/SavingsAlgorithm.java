package solver.initial.savings;

import solver.VRPInstance;

import java.lang.Comparable;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

// Edge class 
class Edge implements Comparable<Edge> {
  int i, j;
  double savings;

  public Edge(VRPInstance vrp, int i, int j) {
    this.i = i;
    this.j = j;
    this.savings = Math.sqrt(
            Math.pow(vrp.xCoordOfCustomer[i], 2) +
                    Math.pow(vrp.yCoordOfCustomer[i], 2)
    ) + Math.sqrt(
            Math.pow(vrp.xCoordOfCustomer[j], 2) +
                    Math.pow(vrp.yCoordOfCustomer[j], 2)
    ) - Math.sqrt(
            Math.pow(vrp.xCoordOfCustomer[i] - vrp.xCoordOfCustomer[j], 2) +
                    Math.pow(vrp.yCoordOfCustomer[i] - vrp.yCoordOfCustomer[j], 2)
    );
  }

  @Override
	public int compareTo(Edge e) {
		return this.savings < e.savings ? 1 : (this.savings > e.savings ? -1 : 0);
	}
}

// Implement Clark-Wright's Savings Algorithm
// NOTE: This may return solutions with too many tours!
public class SavingsAlgorithm {
    VRPInstance vrp;

    ArrayList<Edge> savings_list;
    ArrayList<ArrayList<Integer>> tours;
    ArrayList<ArrayList<Integer>> invalid_tours;

    HashMap<Integer, Integer> is_in_tour;
    HashMap<Integer, Integer> capacity_of_tour;

    double total_cost;

    // Constructor
    public SavingsAlgorithm(VRPInstance vrp) {
        this.vrp = vrp;

        // Create the savings list
        this.savings_list = new ArrayList<>();
        for (int i = 1; i < vrp.numCustomers; i++) {
          for (int j = 1; j < vrp.numCustomers; j++) {
            if (i != j) {
              Edge e = new Edge(vrp, i, j);
              savings_list.add(e);
            }
          }
        }
        Collections.sort(this.savings_list);

        // Initialize other fields
        this.tours = new ArrayList<>();
        this.invalid_tours = new ArrayList<>();

        this.is_in_tour = new HashMap<>();
        for (int i = 0; i < vrp.numCustomers; i++) {
          this.is_in_tour.put(i, -1);
        }

        this.capacity_of_tour = new HashMap<>();
    }


    /*
    Rank the savings s(i, j) and list them in descending order of magnitude. This creates the "savings list." Process the savings list beginning with the topmost entry in the list (the largest s(i, j)).
    For the savings s(i, j) under consideration, include link (i, j) in a route if no route constraints will be violated through the inclusion of (i, j) in a route, and if:
    a. Either, neither i nor j have already been assigned to a route, in which case a new route is initiated including both i and j.
    b. Or, exactly one of the two points (i or j) has already been included in an existing route and that point is not interior to that route (a point is interior to a route if it is not adjacent to the depot D in the order of traversal of points), in which case the link (i, j) is added to that same route.
    c. Or, both i and j have already been included in two different existing routes and neither point is interior to its route, in which case the two routes are merged.
    */
    // Run the algorithm
    public ArrayList<ArrayList<Integer>> run() {
      // Process the savings list in order until it is empty.
      for (Edge e : this.savings_list) {
        // a. If neither are in a route
        if (this.is_in_tour.get(e.i) == -1 && this.is_in_tour.get(e.j) == -1) {
          // Check that these two don't violate capacity of the tour
          if (this.vrp.demandOfCustomer[e.i] + this.vrp.demandOfCustomer[e.j] > this.vrp.vehicleCapacity) continue;

          // Make a new tour with these two - order doesn't matter
          ArrayList<Integer> new_tour = new ArrayList<>();
          new_tour.add(e.i);
          new_tour.add(e.j);
          this.tours.add(new_tour);
          this.is_in_tour.put(e.i, this.tours.size() - 1);
          this.is_in_tour.put(e.j, this.tours.size() - 1);
          this.capacity_of_tour.put(this.tours.size() - 1, this.vrp.demandOfCustomer[e.i] + this.vrp.demandOfCustomer[e.j]);
        }
        // b. If one is in a route
        else if ((this.is_in_tour.get(e.i) != -1 || this.is_in_tour.get(e.j) != -1) && !(this.is_in_tour.get(e.i) != -1 && this.is_in_tour.get(e.j) != -1)) {
          // Check which one is existing
          int oldc = e.i;
          int newc = e.j;
          if (this.is_in_tour.get(e.i) == -1) {
            oldc = e.j;
            newc = e.i;
          }

          // Check if capacity would be violated
          int t_idx = this.is_in_tour.get(oldc);
          if (this.capacity_of_tour.get(t_idx) + this.vrp.demandOfCustomer[newc] > this.vrp.vehicleCapacity) continue;

          // Add if terminal
          ArrayList<Integer> tour = this.tours.get(t_idx);
          if (tour.get(0) == oldc) {
            tour.add(0, newc);
            this.is_in_tour.put(newc, t_idx);
            this.capacity_of_tour.put(t_idx, this.capacity_of_tour.get(t_idx) + this.vrp.demandOfCustomer[newc]);
          } else if (tour.get(tour.size() - 1) == oldc) {
            tour.add(newc);
            this.is_in_tour.put(newc, t_idx);
            this.capacity_of_tour.put(t_idx, this.capacity_of_tour.get(t_idx) + this.vrp.demandOfCustomer[newc]);
          }
        }

        // c. If both are in a route
        else if (this.is_in_tour.get(e.i) != -1 && this.is_in_tour.get(e.j) != -1) {
          // Get the tours its in
          int ti_idx = this.is_in_tour.get(e.i);
          int tj_idx = this.is_in_tour.get(e.j);
          if (ti_idx == tj_idx) continue;

          // Check if merging would violate capacity
          if (this.capacity_of_tour.get(ti_idx) + this.capacity_of_tour.get(tj_idx) > this.vrp.vehicleCapacity) continue;

          // Merge tour j into tour i if both are terminal
          boolean merging = false;
          ArrayList<Integer> tour_i = this.tours.get(ti_idx);
          ArrayList<Integer> tour_j = this.tours.get(tj_idx);

          // TODO: Can optimize here
          if (tour_i.get(0) == e.i) {
            if (tour_j.get(0) == e.j) {
              Collections.reverse(tour_i);
              merging = true;
            } else if (tour_j.get(tour_j.size() - 1) == e.j) {
              Collections.reverse(tour_i);
              Collections.reverse(tour_j);
              merging = true;
            }
          } else if (tour_i.get(tour_i.size() - 1) == e.i) {
            if (tour_j.get(0) == e.j) {
              merging = true;
            } else if (tour_j.get(tour_j.size() - 1) == e.j) {
              Collections.reverse(tour_j);
              merging = true;
            }
          }

          // If mergeing...
          if (merging) {
            for (Integer c : tour_j) {
              tour_i.add(c);
              this.is_in_tour.put(c, ti_idx);
            }

            this.capacity_of_tour.put(ti_idx, this.capacity_of_tour.get(ti_idx) + this.capacity_of_tour.get(tj_idx));
            this.capacity_of_tour.remove(tj_idx);
            this.invalid_tours.add(tour_j);
          }
        }
      }

      // Assign those not in a tour yet to a singleton tour
      for (int i = 1; i < vrp.numCustomers; i++) {
        if (this.is_in_tour.get(i) == -1) {
          ArrayList<Integer> new_tour = new ArrayList<>();
          new_tour.add(i);
          this.tours.add(new_tour);
          this.is_in_tour.put(i, this.tours.size() - 1);
          this.capacity_of_tour.put(this.tours.size() - 1, this.vrp.demandOfCustomer[i]);
        }
      }

      // If there is too many tours for cars, combine tours.
      // NOTE: This combination strategy is NOT optimal!
      // TODO: make more optimal
      for (int x = 0; x < (this.tours.size() - this.invalid_tours.size()) - this.vrp.numVehicles; x++) {
        for (int i = 0; i < this.tours.size(); i++) {
          for (int j = 0; j < this.tours.size(); j++) {
            if (i == j) continue;
            // Check validity
            ArrayList<Integer> to_remove = this.tours.get(i);
            ArrayList<Integer> merge_into = this.tours.get(j);
            if (this.invalid_tours.contains(to_remove) || this.invalid_tours.contains(merge_into)) continue;
            if (this.capacity_of_tour.get(i) + this.capacity_of_tour.get(j) > this.vrp.vehicleCapacity) continue;

            // Merge!
            for (Integer c : to_remove) {
              merge_into.add(c);
              this.is_in_tour.put(c, j);
            }
            this.capacity_of_tour.put(j, this.capacity_of_tour.get(i) + this.capacity_of_tour.get(j));
            this.invalid_tours.add(to_remove);
          }
        }
      }

      // Check if still too many tours
      if (this.tours.size() - this.invalid_tours.size() > this.vrp.numVehicles) {
        // return String.format("ERROR(too many tours) - tours: %d vehicles: %d", this.tours.size() + this.invalid_tours.size(), this.vrp.numVehicles);
        return null;
      }

      // Calculate the total cost and generate result by running through each tour.

      // STRING PARSER FOR OUTPUT
      // String result = "";
      // this.total_cost = 0.0f;
      // for (ArrayList<Integer> tour : this.tours) {
      //   int curr = 0;
      //   for (Integer x : tour) {
      //     this.total_cost += Math.sqrt(
      //       Math.pow(vrp.xCoordOfCustomer[curr] - vrp.xCoordOfCustomer[x], 2) +
      //               Math.pow(vrp.yCoordOfCustomer[curr] - vrp.yCoordOfCustomer[x], 2)
      //     );
      //     result += String.format("%d ", curr);
      //     curr = x;
      //   }
      //   this.total_cost += Math.sqrt(
      //     Math.pow(vrp.xCoordOfCustomer[curr] - vrp.xCoordOfCustomer[0], 2) +
      //             Math.pow(vrp.yCoordOfCustomer[curr] - vrp.yCoordOfCustomer[0], 2)
      //   );
      //   result += String.format("%d %d ", curr, 0);
      // }

      // // Add to the result the empty trucks
      // for (int i = this.tours.size(); i < this.vrp.numVehicles; i++) {
      //   result += "0 0 ";
      // }

      // STRING PARSER FOR VISUALIZER
      // String result = "";
      // this.total_cost = 0.0f;
      // for (ArrayList<Integer> tour : this.tours) {
      //   // skip invalid tours
      //   if (this.invalid_tours.contains(tour)) continue;

      //   int curr = 0;
      //   for (Integer x : tour) {
      //     this.total_cost += Math.sqrt(
      //       Math.pow(vrp.xCoordOfCustomer[curr] - vrp.xCoordOfCustomer[x], 2) +
      //               Math.pow(vrp.yCoordOfCustomer[curr] - vrp.yCoordOfCustomer[x], 2)
      //     );
      //     result += String.format("%d ", curr);
      //     curr = x;
      //   }
      //   this.total_cost += Math.sqrt(
      //     Math.pow(vrp.xCoordOfCustomer[curr] - vrp.xCoordOfCustomer[0], 2) +
      //             Math.pow(vrp.yCoordOfCustomer[curr] - vrp.yCoordOfCustomer[0], 2)
      //   );
      //   result += String.format("%d %d\n", curr, 0);
      // }
      // System.out.println(result);
      // return result.substring(0, result.length() - 1);

      return this.tours;
    }

    public double get_cost() {
      return this.total_cost;
    }
}
