package pulse.search;


import pulse.math.ParameterVector;
import pulse.problem.schemes.solvers.SolverException;

/**
 * An interface for dealing with optimisation variables. The variables are
 * collected in {@code IndexedVector}s according to the pattern set up by a list
 * of {@code Flag}s.
 */
public interface Optimisable {

    /**
     * Assigns parameter values of this {@code Optimisable} using the
     * optimisation vector {@code params}. Only those parameters will be
     * updated, the types of which are listed as indices in the {@code params}
     * vector.
     *
     * @param input the optimisation vector, containing a similar set of
     * parameters to this {@code Problem}
     * @throws SolverException if {@code params} contains invalid parameter
     * values
     * @see pulse.util.PropertyHolder.listedTypes()
     */
    public void assign(ParameterVector input) throws SolverException;

    /**
     * Calculates the vector argument defined on
     * <math><b>R</b><sup>n</sup></math>
     * to the scalar objective function for this {@code Optimisable}.
     *
     * @param output the output vector where the result will be stored
     */
    public void optimisationVector(ParameterVector output);

}
