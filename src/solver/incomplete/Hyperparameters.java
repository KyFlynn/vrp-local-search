package solver.incomplete;

public final class Hyperparameters {
    private Hyperparameters() {}
    // Number of iterations for local searcher
    public static final int numIter = Integer.MAX_VALUE;

    // Probability p for RandomizedIterativeImprovement
    public static final double p = 0.0;

//    // Seed of heuristics randomization
//    public static final long seed = (long) 1234567890;
}