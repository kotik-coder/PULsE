package pulse.problem.schemes.solvers;

import java.io.Serializable;
import pulse.problem.statements.Problem;

/**
 * A solver interface which provides the capability to use the {@code solve}
 * method on a {@code Problem}. This interface is implemented by the subclasses
 * of {@code DifferenceSCheme}.
 *
 * @param <T> an instance of Problem
 */
public interface Solver<T extends Problem> extends Serializable {

    /**
     * Calculates the solution of the {@code t} and stores it in the respective
     * {@code HeatingCurve}.
     *
     * @param problem - an accepted instance of {@code T}
     * @throws SolverException
     */
    public void solve(T problem) throws SolverException;

}
