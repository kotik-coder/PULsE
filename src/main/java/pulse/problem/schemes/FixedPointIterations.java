package pulse.problem.schemes;

import static java.lang.Math.abs;
import java.util.Arrays;
import pulse.problem.schemes.solvers.SolverException;

/**
 * @see <a href="https://en.wikipedia.org/wiki/Fixed-point_iteration">Wiki
 * page</a>
 *
 */
public interface FixedPointIterations {

    /**
     * Performs iterations until the convergence criterion is satisfied.The
 latter consists in having a difference two consequent iterations of V
 less than the specified error. At the end of each iteration, calls
 {@code finaliseIteration()}.
     *
     * @param V the calculation array
     * @param error used in the convergence criterion
     * @param m time step
     * @throws pulse.problem.schemes.solvers.SolverException if the calculation failed
     * @see finaliseIteration()
     * @see iteration()
     */
    public default void doIterations(double[] V, final double error, final int m) throws SolverException {

        final int N = V.length - 1;

        for (double V_0 = error + 1, V_N = error + 1; abs(V[0] - V_0) > error
                || abs(V[N] - V_N) > error; finaliseIteration(V)) {

            V_N = V[N];
            V_0 = V[0];
            iteration(m);

        }
    }

    /**
     * Performs an iteration at time {@code m}
     *
     * @param m time step
     * @throws pulse.problem.schemes.solvers.SolverException if the calculation failed
     */
    public void iteration(final int m) throws SolverException;

    /**
     * Finalises the current iteration.By default, does nothing.
     *
     * @param V the current iteration
     * @throws pulse.problem.schemes.solvers.SolverException if the calculation failed
     */
    public default void finaliseIteration(double[] V) throws SolverException {
        final double threshold = 1E6;
        double sum = Arrays.stream(V).sum();
        if( sum > threshold || !Double.isFinite(sum) )
            throw new SolverException("Invalid solution values in V array");
    }

}
