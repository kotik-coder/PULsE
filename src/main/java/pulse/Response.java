package pulse;

import java.io.Serializable;
import pulse.math.Segment;
import pulse.problem.schemes.solvers.SolverException;
import pulse.search.GeneralTask;
import pulse.search.statistics.OptimiserStatistic;

public interface Response extends Serializable {

    public double evaluate(double t);

    public Segment accessibleRange();

    /**
     * Calculates the value of the objective function used to identify the
     * current state of the optimiser.
     *
     * @param task
     * @return the value of the objective function in the current state
     * @throws pulse.problem.schemes.solvers.SolverException
     */
    public double objectiveFunction(GeneralTask task) throws SolverException;

    public OptimiserStatistic getOptimiserStatistic();

}
