package solver.util;

import solver.VRPInstance;

import java.util.ArrayList;

public class Route {

    VRPInstance vrp;
    public int vehicle;
    public DoublyLinkedCycle routeCycle;
    public double distance;
    public int demand;
    public int nCustomers;

    public Route(VRPInstance instance, int v, Node[] customerNodes, ArrayList<Integer> customers) {
        vrp = instance;
        vehicle = v;
        routeCycle =  new DoublyLinkedCycle(v, customerNodes, customers);
        distance = getTotalDistance();
        demand = getTotalDemand();
        if (customers != null) {
            nCustomers = customers.size();
        } else {
            nCustomers = 0;
        }
    }

    public double getTotalDistance() {
        double totalDistance = 0.0;
        // First customer after depot retrieved.
        Node customer = routeCycle.depot.next;
        // Case where route is empty.
        if (customer == routeCycle.depot) {
            return totalDistance;
        }
        // Exit from depot distance
        totalDistance += euclideanDistance(routeCycle.depot, customer);
        // While traveling to another customer
        while (customer.next != routeCycle.depot) {
            Node next = customer.next;
            totalDistance += euclideanDistance(customer, next);
            customer = next;
        }
        // Return to depot distance.
        assert customer.next == routeCycle.depot;
        totalDistance += euclideanDistance(customer, routeCycle.depot);
        return totalDistance;
    }

    public int getTotalDemand() {
        int totalDemand = 0;
        // First customer after depot retrieved.
        Node customer = routeCycle.depot.next;
        // Case where route is empty.
        if (customer == routeCycle.depot) {
            return totalDemand;
        }
        // Sum demand over customers
        do {
            totalDemand += vrp.demandOfCustomer[customer.customer];
            customer = customer.next;
        } while (customer != routeCycle.depot);
        return totalDemand;
    }

    public void add(Node n, Node newLocPrev, double addedDist) {
        routeCycle.addNode(n, newLocPrev);
        distance += addedDist;
        demand += vrp.demandOfCustomer[n.customer];
        nCustomers += 1;
    }

    public void remove(Node n, double removedDist) throws Exception {
        routeCycle.removeNode(n);
        distance -= removedDist;
        demand -= vrp.demandOfCustomer[n.customer];
        nCustomers -= 1;
    }

    public void swap(Node n1, Node n2, Route r2, double addedDist1, double addedDist2) throws Exception {
        routeCycle.swapNodes(n1, n2);
        distance += addedDist1;
        r2.distance += addedDist2;
        demand -= vrp.demandOfCustomer[n1.customer];
        demand += vrp.demandOfCustomer[n2.customer];
        r2.demand += vrp.demandOfCustomer[n1.customer];
        r2.demand -= vrp.demandOfCustomer[n2.customer];
    }

    public double euclideanDistance(Node c1, Node c2) {
        double diffX = vrp.xCoordOfCustomer[c1.customer] - vrp.xCoordOfCustomer[c2.customer];
        double diffY = vrp.yCoordOfCustomer[c1.customer] - vrp.yCoordOfCustomer[c2.customer];
        return Math.sqrt(diffX * diffX + diffY * diffY);
    }

    public void printRoute() {
        Node depot = routeCycle.depot;
        System.out.println(String.format("Vehicle: %d (demand=%d)", vehicle, demand));
        System.out.print(String.format("%d <- %d -> %d (v:%d)", depot.prev.customer, depot.customer,
                depot.next.customer, depot.vehicle));
        Node curr = depot;
        while (curr.next != depot) {
            curr = curr.next;
            System.out.print(String.format(", %d <- %d -> %d (v:%d)", curr.prev.customer, curr.customer,
                    curr.next.customer, curr.vehicle));
        }
        System.out.println("");
    }
}
