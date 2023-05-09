package solver.incomplete;

import solver.VRPInstance;
import solver.util.Node;
import solver.util.Route;

public class Checker {

    public boolean check(VRPInstance vrp, Route[] routes) {
        boolean works = true;
        works = works && routeNumCheck(vrp, routes);
        works = works && customerServedCheck(vrp, routes);
        for (Route r : routes) {
            works = works && depotCheck(r);
            works = works && sameVehicleCheck(r);
            works = works && tourCheck(vrp, r);
            works = works && feasibilityCheck(vrp, r);
            works = works && distanceCheck(vrp, r);
        }
        return works;
    }

    // Correct number of routes
    public boolean routeNumCheck(VRPInstance vrp, Route[] routes) {
        if (routes.length != vrp.numVehicles) {
            System.out.println("Incorrect number of tours for number of vehicles.");
            return false;
        }
        return true;
    }

    // Every customer is served exactly once
    public boolean customerServedCheck(VRPInstance vrp, Route[] routes) {
        int[] customerServed = new int[vrp.numCustomers];
        for (Route r : routes) {
            Node depot = r.route.depot;
            if (depot.next == depot) {
                continue;
            }
            Node curr = depot;
            while (curr.next != depot) {
                curr = curr.next;
                customerServed[curr.customer] += 1;
            }
        }
        for (int c = 0; c < vrp.numCustomers; c++) {
            if (customerServed[c] != 1) {
                System.out.println(String.format("Customer %d is not served by the set of tours:", c));
                for (Route r : routes) {
                    printTour(r);
                }
                return false;
            }
        }
        return true;
    }


    // Depot implemented correctly
    public boolean depotCheck(Route r) {
        if (r.route.depot.customer != 0) {
            System.out.println("Depot not set to customer 0.");
            printTour(r);
            return false;
        } else {
            return true;
        }
    }


    // Same vehicle check
    public boolean sameVehicleCheck(Route r) {
        Node depot = r.route.depot;
        if (depot.next == depot) {
            return true;
        }
        int v = r.vehicle;
        Node curr = depot;
        while (curr.next != depot) {
            curr = curr.next;
            if (curr.vehicle != v) {
                System.out.println(String.format("Customer in tour thinks it's in vehicle %d but is in %d",
                        curr.vehicle, v));
                printTour(r);
                return false;
            }
        }
        return true;
    }

    // All routes are tours
    public boolean tourCheck(VRPInstance vrp, Route r) {
        Node depot = r.route.depot;
        if (depot.next == depot) {
            if (depot.prev != depot) {
                System.out.println("Depot not pointing to itself correctly.");
                printTour(r);
                return false;
            } else {
                return true;
            }
        }
        Node curr = depot;
        int i = 0;
        while (curr.next != depot) {
            i += 1;
            if (curr.next.prev != curr) {
                System.out.println("Previous pointer incorrect in cycle");
                printTour(r);
                return false;
            }
            curr = curr.next;
            if (curr == null) {
                System.out.println("Found null in 'next' field of node.");
                printTour(r);
                return false;
            }
            if (i > vrp.numCustomers) {
                System.out.println("Tour longer than number of customers.");
                printTour(r);
                return false;
            }
        }
        return true;
    }

    // Feasibility check
    public boolean feasibilityCheck(VRPInstance vrp, Route r) {
        Node depot = r.route.depot;
        if (depot.next == depot) {
            return true;
        }
        Node curr = depot;
        int demand = 0;
        while (curr.next != depot) {
            curr = curr.next;
            demand += vrp.demandOfCustomer[curr.customer];
        }
        if (demand > vrp.vehicleCapacity) {
            System.out.println("Vehicle capacity exceeded.");
            printTour(r);
            return false;
        }
        return true;
    }

    // Distance check?
    public boolean distanceCheck(VRPInstance vrp, Route r) {
        Node depot = r.route.depot;
        if (depot.next == depot) {
            return true;
        }
        Node curr = depot;
        double distance = 0;
        while (curr.next != depot) {
            Node next = curr.next;
            double diffX = vrp.xCoordOfCustomer[curr.customer] - vrp.xCoordOfCustomer[next.customer];
            double diffY = vrp.yCoordOfCustomer[curr.customer] - vrp.yCoordOfCustomer[next.customer];
            distance += Math.sqrt(diffX * diffX + diffY * diffY);
            curr = next;;
        }
        double diffX = vrp.xCoordOfCustomer[curr.customer] - vrp.xCoordOfCustomer[depot.customer];
        double diffY = vrp.yCoordOfCustomer[curr.customer] - vrp.yCoordOfCustomer[depot.customer];
        distance += Math.sqrt(diffX * diffX + diffY * diffY);
        if (distance != r.distance) {
            System.out.println(String.format("Distance value %.2f but route thinks it's %.2f", distance, r.distance));
            printTour(r);
            return false;
        }
        return true;
    }


    public void printTour(Route r) {
        System.out.println("Issue with tour —> DFS looks like:");
        Node depot = r.route.depot;
        System.out.print(String.format("%d <- %d -> %d, ", depot.prev, depot, depot.next));
        Node curr = depot;
        while (curr.next != depot) {
            curr = curr.next;
            System.out.print(String.format("%d <- %d -> %d, ", curr.prev, curr, curr.next));
        }
    }

}
