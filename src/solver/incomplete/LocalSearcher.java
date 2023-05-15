package solver.incomplete;

import solver.VRPInstance;
import solver.initial.randomfeasible.CPSolutionFinder;
import solver.initial.savings.SavingsAlgorithm;
import solver.initial.sweep.SweepAlgorithm;
import solver.util.Node;
import solver.util.Route;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Random;


class Proposed {
    Node n1, n2;
    int move; // 0 for relocation, 1 for swap
    double delta;

    public Proposed(Node n1, Node n2, int move, double delta) {
        this.n1 = n1;
        this.n2 = n2;
        this.move = move;
        this.delta = delta;
    };
}
