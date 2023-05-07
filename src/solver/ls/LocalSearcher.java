package solver.ls;

import solver.VRPInstance;

public abstract class LocalSearcher {

    VRPInstance vrp;
    //    IN VRP:
    //    int numCustomers;                // the number of customers
    //    int numVehicles;                 // the number of vehicles
    //    int vehicleCapacity;             // the capacity of the vehicles
    //    int[] demandOfCustomer;          // the demand of each customer
    //    double[] xCoordOfCustomer;       // the x coordinate of each customer
    //    double[] yCoordOfCustomer;       // the y coordinate of each customer
    Route[] vehicleRoutes;

    public LocalSearcher(VRPInstance instance) {
        vrp = instance;
        vehicleRoutes = new Route[vrp.numVehicles];
    }

    public void compare() {

    }

    public abstract void initSolution();

    public abstract void step();

}
