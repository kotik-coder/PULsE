package pulse.search.linear;

import static java.lang.Math.abs;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.LINEAR_RESOLUTION;

import java.util.Set;

import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.math.linear.Vector;
import pulse.problem.schemes.solvers.SolverException;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.SearchTask;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

/**
 * The most basic {@code LinearSolver} class, which defines the notion of the
 * linear resolution, defines the method signature for estimating the step of
 * the linear search (i.e., the position of the minimum), and provides a simple
 * algorithm to initialise the calculation domain.
 *
 */
public abstract class LinearOptimiser extends PropertyHolder implements Reflexive {

    protected static double searchResolution = (double) def(LINEAR_RESOLUTION).getValue();
    private final static double EPS = 1E-15;

    protected LinearOptimiser() {
        super();
    }

    /**
     * Finds the minimum of the target function on the {@code domain}
     * {@code Segment}.
     *
     * @param task the target function is the sum of squared residuals (SSR) for
     * this {@code task}
     * @return a double, representing the step magnitude that needs to be
     * multiplied by the direction of the search determined previously using the
     * {@code PathSolver} to arrive at the next set of parameters corresponding
     * to a lower SSR value of this {@code task}
     * @throws SolverException
     */
    public abstract double linearStep(SearchTask task) throws SolverException;

    /**
     * Sets the domain for this linear search on {@code p}.
     * <p>
     * The domain is defined as a {@code Segment} {@code [0; max]}, where
     * {@code max} determines the maximum magnitude of the {@code linearStep}.
     * This value is calculated initially as
     * <code>max = 0.5*x<sub>i</sub>/p<sub>i</sub></code>, where <i>i</i> is the
     * index of the {@code DIFFUSIVITY NumericProperty}. Later it is corrected
     * to ensure that the change in the {@code HEAT_LOSS}
     * {@code NumericProperty} is less than unity.
     * </p>
     *
     * @param x the current set of parameter
     * @param p the result of the direction search with the {@code PathSolver}
     * @return a {@code Segment} defining the domain of this search
     * @see pulse.search.direction.PathSolver.direction(SearchTask)
     */
    public static Segment domain(ParameterVector x, Vector p) {
        double alphaMax = Double.POSITIVE_INFINITY;
        double alpha;

        for (int i = 0; i < x.dimension(); i++) {

            final double component = p.get(i);

            //check if zero
            if (component < EPS && component > -EPS) {
                continue;
            }

            var bound = x.getTransformedBounds(i);

            alpha = abs(
                    ((component > 0 ? bound.getMaximum() : bound.getMinimum()) - x.get(i))
                    / component);

            if (Double.isFinite(alpha) && alpha < alphaMax) {
                alphaMax = alpha;
            }

        }

        //check that alphaMax is not zero! otherwise the optimise will crash
        return new Segment(0.0, 
                Math.max(alphaMax, 1E-10));

    }

    /**
     * <p>
     * The linear resolution determines the minimum distance between any two
     * points belonging to the {@code domain} of this search while they still
     * are considered separate. In case of a partitioning method, e.g. the
     * golden-section search, this determines the partitioning limit. Note
     * different {@code PathSolver}s can have different sensitivities to the
     * linear search and may require different linear resolutions to work
     * effectively.
     * </p>
     *
     * @return a {@code NumericProperty} with the current value of the linear
     * resolution
     * @see domain(IndexedVector,IndexedVector,Vector)
     */
    public static NumericProperty getLinearResolution() {
        return derive(LINEAR_RESOLUTION, searchResolution);
    }

    public static void setLinearResolution(NumericProperty searchError) {
        LinearOptimiser.searchResolution = (double) searchError.getValue();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    /**
     * The {@code LINEAR_RESOLUTION} is the single listed parameter for this
     * class.
     *
     * @see pulse.properties.NumericPropertyKeyword
     */
    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(LINEAR_RESOLUTION);
        return set;
    }
    
    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        if (type == LINEAR_RESOLUTION) {
            setLinearResolution(property);
        }
    }

}
