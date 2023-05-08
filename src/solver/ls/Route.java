package solver.ls;

import solver.VRPInstance;

public class Route {

    VRPInstance vrp;
    int vehicle;
    DoublyLinkedCycle route;
    public double distance;
    public int demand;

    public Route(VRPInstance instance, int v, Node[] customerNodes, int[] customers) {
        vrp = instance;
        vehicle = v;
        route =  new DoublyLinkedCycle(v, customerNodes, customers);
        distance = getTotalDistance();
        demand = getTotalDemand();
    }

    public double getTotalDistance() {
        double totalDistance = 0.0;
        // First customer after depot retrieved.
        Node customer = route.depot.next;
        // Case where route is empty.
        if (customer == route.depot) {
            return totalDistance;
        }
        // Exit from depot distance
        totalDistance += euclideanDistance(route.depot, customer);
        // While traveling to another customer
        while (customer.next != route.depot) {
            Node next = customer.next;
            totalDistance += euclideanDistance(customer, next);
            customer = next;
        }
        // Return to depot distance.
        assert customer.next == route.depot;
        totalDistance += euclideanDistance(customer, route.depot);
        return totalDistance;
    }

    public int getTotalDemand() {
        int totalDemand = 0;
        // First customer after depot retrieved.
        Node customer = route.depot.next;
        // Case where route is empty.
        if (customer == route.depot) {
            return totalDemand;
        }
        // Sum demand over customers
        do {
            totalDemand += vrp.demandOfCustomer[customer.val];
            customer = customer.next;
        } while (customer != route.depot);
        return totalDemand;
    }

    public void add(Node n, Node newLocPrev, double addedDist) {
        route.addNode(n, newLocPrev);
        distance += addedDist;
        demand += vrp.demandOfCustomer[n.val];
    }

    public void remove(Node n, double removedDist) throws Exception {
        route.removeNode(n);
        distance -= removedDist;
        demand -= vrp.demandOfCustomer[n.val];
    }

    public double euclideanDistance(Node c1, Node c2) {
        double diffX = vrp.xCoordOfCustomer[c1.val] - vrp.xCoordOfCustomer[c2.val];
        double diffY = vrp.yCoordOfCustomer[c1.val] - vrp.yCoordOfCustomer[c2.val];
        return Math.sqrt(diffX * diffX + diffY * diffY);
    }
}
