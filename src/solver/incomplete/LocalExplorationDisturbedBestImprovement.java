package solver.incomplete;

import solver.VRPInstance;
import solver.util.Route;

import java.util.HashSet;
import java.util.Hashtable;

import java.io.FileNotFoundException;

public class LocalExplorationDisturbedBestImprovement extends DisturbedBestImprovement {

    Hashtable<Integer, LocalMinimum> objValToLocalMinTable;
    int intLastReachedMinObjVal = 0;
    int maxNumTimesReached = 10;

    public int maxDisturbances = initNumDisturbances;
    public double timeBestReached = 0.0;

    public LocalExplorationDisturbedBestImprovement(VRPInstance instance) throws FileNotFoundException {
        super(instance);
        objValToLocalMinTable = new Hashtable<>();
        objValToLocalMinTable.put(intLastReachedMinObjVal, new LocalMinimum(-1));
    }

    public int intObjValue(double val) {
        return (int) val;
    }

    public void updateLastMinimum(int truncatedIntCurrObjVal) {
        LocalMinimum lastMin = objValToLocalMinTable.get(intLastReachedMinObjVal);
        // If current obj value has been reached from last min, update exploration parameters
        if (lastMin.minimaReached.contains(truncatedIntCurrObjVal)) {
            lastMin.numTimesReachedNeighbors += 1;
            if (lastMin.numTimesReachedNeighbors == maxNumTimesReached) {
                lastMin.numDisturbances = updateDisturbanceNumber(lastMin.numDisturbances);
                if (lastMin.numDisturbances > maxDisturbances) {
                    maxDisturbances = lastMin.numDisturbances;
                }
                lastMin.numTimesReachedNeighbors = 0;
            }
        } else {
            // If it hasn't, reset exploration parameters and record new local minimum
            lastMin.minimaReached.add(truncatedIntCurrObjVal);
            lastMin.numTimesReachedNeighbors = 0;
        }
    }

    public void disturbLocalMinimum(double objVal) throws Exception {
        // Multiply and truncate current objective val into integer for ease of use
        int intCurrObjVal = intObjValue(objVal);
        // Update exploration parameters of previously reached local minimum
        updateLastMinimum(intCurrObjVal);
        // Set last local minimum to current
        intLastReachedMinObjVal = intCurrObjVal;
        // Retrieve current local minimum from hashtable by objective value
        LocalMinimum currMin = objValToLocalMinTable.get(intCurrObjVal);
        // System.out.println(objValToLocalMinTable.size());
        if (currMin != null) {
            applyDisturbances(currMin.numDisturbances);
        } else { // Never been to this local minimum -> begin disturbance
            LocalMinimum newMin = new LocalMinimum(intCurrObjVal);
            objValToLocalMinTable.put(intCurrObjVal, newMin);
            // System.out.println(String.format("New local minimum added to table with value: %d", truncatedIntCurrObjVal));
            // System.out.println("Map looks like:");
            // for (Map.Entry e : objValToLocalMinTable.entrySet()) {
            //     System.out.println(e);
            // }
            applyDisturbances(newMin.numDisturbances);
        }
    }

    public double solve() throws Exception {
        timer.start();
        // Continuously find and disturb local minima using each recorded minimum's exploration parameters
        boolean localMinReached;
        while (timer.getTime() < runtime) {
            localMinReached = step();
            // Update best
            if (currObjVal < bestObjVal) {
                bestObjVal = currObjVal;
                bestRoutes = vehicleRoutes;
                timeBestReached = timer.getTime();
            }
            if (localMinReached) {
                disturbLocalMinimum(currObjVal);
            }
        }
        return bestObjVal;
    }
}


class LocalMinimum {

    double truncatedIntObjVal;
    int numDisturbances = 4;
    HashSet<Integer> minimaReached = new HashSet<>();
    int numTimesReachedNeighbors = 0;

    public LocalMinimum(double val) {
        truncatedIntObjVal = val;
    }
}