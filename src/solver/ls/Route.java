package solver.ls;

import solver.VRPInstance;

public class Route {

    VRPInstance vrp;
    DoublyLinkedCycle route;
    public double distance;
    public int demand;

    public Route(VRPInstance instance, int[] customers) {
        vrp = instance;
        // Initialize linked list starting at depot (customer 0)
        route =  new DoublyLinkedCycle(0, customers);
        distance = getTotalDistance();
        demand = getTotalDemand();
    }

//    public void removeCustomer()

    public double getTotalDistance() {
        double totalDistance = 0.0;
        Node customer = route.start.next;
        // Case where route is empty
        if (customer == route.start) {
            return totalDistance;
        }
        // Exit from depot distance
        totalDistance += euclideanDistance(route.start, customer);
        // While traveling to another customer
        while (customer.next != route.start) {
            Node next = customer.next;
            totalDistance += euclideanDistance(customer, next);
            customer = next;
        }
        // Return to depot distance
        assert customer.next == route.start;  // TODO: Remove once working
        totalDistance += euclideanDistance(customer, route.start);
        return totalDistance;
    }

    public int getTotalDemand() {
        int totalDemand = 0;
        Node customer = route.start.next;
        // Case where route is empty
        if (customer == route.start) {
            return totalDemand;
        }
        // Sum demand over customers
        do {
            totalDemand += vrp.demandOfCustomer[customer.val];
            customer = customer.next;
        } while (customer != route.start);
        return totalDemand;
    }

    public double euclideanDistance(Node c1, Node c2) {
        double diffX = vrp.xCoordOfCustomer[c1.val] - vrp.xCoordOfCustomer[c2.val];
        double diffY = vrp.yCoordOfCustomer[c1.val] - vrp.yCoordOfCustomer[c2.val];
        return Math.sqrt(Math.pow(diffX, 2) + Math.pow(diffY, 2));
    }
}
