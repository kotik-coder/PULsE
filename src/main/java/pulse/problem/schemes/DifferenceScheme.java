package pulse.problem.schemes;

import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.TIME_LIMIT;

import java.util.Set;

import pulse.problem.laser.DiscretePulse;
import pulse.problem.schemes.solvers.SolverException;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

/**
 * A {@code DifferenceScheme} is an abstract class that declares general methods
 * for converting a {@code Problem} to a set of algebraic operations on a
 * {@code Grid}. The {@code Grid} object defines the time and coordinate
 * partitioning, adjusted to ensure a stable or conditionally-stable behaviour
 * of the solution. The {@code Grid} is also used to define a
 * {@code DiscretePulse} function.
 *
 * @see pulse.problem.schemes.Grid
 * @see pulse.problem.laser.DiscretePulse
 */
public abstract class DifferenceScheme extends PropertyHolder implements Reflexive {

    private DiscretePulse discretePulse;
    private Grid grid;

    private double timeLimit;
    private double pls;
    private int timeInterval;

    private static boolean hideDetailedAdjustment = true;

    private final static double EPS = 1e-7; // a small value ensuring numeric stability

    /**
     * A constructor which merely sets the time limit to its default value.
     */
    protected DifferenceScheme() {
        setTimeLimit(def(TIME_LIMIT));
    }

    /**
     * A constructor for setting the time limit to a pre-set value.
     *
     * @param timeLimit the calculation time limit
     */
    protected DifferenceScheme(NumericProperty timeLimit) {
        setTimeLimit(timeLimit);
    }

    public void initFrom(DifferenceScheme another) {
        this.grid = grid.copy();
        this.timeLimit = another.timeLimit;
        this.timeInterval = another.timeInterval;
    }

    /**
     * Copies the {@code Grid} and {@code timeLimit} from {@code df}.
     *
     * @param df the DifferenceScheme to copy from
     */
    public void copyFrom(DifferenceScheme df) {
        this.grid = df.getGrid().copy();
        discretePulse = null;
        timeLimit = df.timeLimit;
    }

    /**
     * <p>
     * Contains preparatory steps to ensure smooth running of the solver.This
     * includes creating a {@code DiscretePulse}object and adjusting the grid of
     * this scheme to match the {@code DiscretePulse}created for this
     * {@code problem} Finally, a heating curve is cleared from the previously
     * calculated values.</p>
     * <p>
     * All subclasses of {@code DifferenceScheme} should override and explicitly
     * call this superclass method where appropriate.
     * </p>
     *
     * @param problem the heat problem to be solved
     * @throws pulse.problem.schemes.solvers.SolverException
     * @see pulse.problem.schemes.Grid.adjustTo()
     */
    protected void prepare(Problem problem) throws SolverException {
        if (discretePulse == null) {
            discretePulse = problem.discretePulseOn(grid);
        }   
        discretePulse.recalculate();
        clearArrays();
    }

    public void runTimeSequence(Problem problem) throws SolverException {
        runTimeSequence(problem, 0, timeLimit);
    }

    public void scaleSolution(Problem problem) {
        var curve = problem.getHeatingCurve();
        final double maxTemp = (double) problem.getProperties().getMaximumTemperature().getValue();
        //curve.scale(maxTemp / curve.apparentMaximum());
        curve.scale(maxTemp);
    }

    public void runTimeSequence(Problem problem, final double offset, final double endTime) throws SolverException {
        var curve = problem.getHeatingCurve();
        curve.clear();

        int numPoints = (int) curve.getNumPoints().getValue();

        final double startTime   = (double) curve.getTimeShift().getValue();
        final double timeSegment = (endTime - startTime - offset) / problem.getProperties().timeFactor();

        double tau = grid.getTimeStep();
        final double dt = timeSegment / (numPoints - 1);
        timeInterval = Math.max( (int) (dt / tau), 1);

        double wFactor = timeInterval * tau * problem.getProperties().timeFactor();

        // First point (index = 0) is always (0.0, 0.0)
        curve.addPoint(0.0, 0.0);

        double nextTime;
        int previous;

        /*
         * The outer cycle iterates over the number of points of the HeatingCurve
         */
        for (previous = 1, nextTime = offset; nextTime < endTime || !curve.isFull();
                previous += timeInterval) {

            /*
             * Two adjacent points of the heating curves are separated by timeInterval on
	     * the time grid. Thus, to calculate the next point on the heating curve,
	     * timeInterval/tau time steps have to be made first.
             */
            timeSegment(previous, previous + timeInterval);
            nextTime += wFactor;
            curve.addPoint(nextTime, signal());
        }

        curve.copyToLastCalculation();
        scaleSolution(problem);
    }

    private void timeSegment(final int m1, final int m2) throws SolverException {
        for (int m = m1; m < m2 && normalOperation(); m++) {
            prepareStep(m);     //prepare
            timeStep(m);        //calculate
            finaliseStep();     //finalise
        }
    }

    public double pulse(final int m) {
        return getDiscretePulse().laserPowerAt((m - EPS) * getGrid().getTimeStep());
    }

    /**
     * Do preparatory calculations that depend only on the time variable, e.g.,
     * calculate the pulse power.
     *
     * @param m the time step number
     */
    public void prepareStep(int m) {
        pls = pulse(m);
    }

    public boolean normalOperation() {
        return true;
    }

    /**
     * The superclass only lists the {@code TIME_LIMIT} property.
     */
    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(TIME_LIMIT);
        return set;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    /**
     * Gets the discrete representation of {@code Pulse} on the {@code Grid}.
     *
     * @return the discrete pulse
     * @see pulse.problem.statements.Pulse
     */
    public final DiscretePulse getDiscretePulse() {
        return discretePulse;
    }

    /**
     * Gets the {@code Grid} object defining partioning used in this
     * {@code DifferenceScheme}
     *
     * @return the grid
     */
    public final Grid getGrid() {
        return grid;
    }

    /**
     * Sets the grid and adopts it as its child.
     *
     * @param grid the grid
     */
    public final void setGrid(Grid grid) {
        this.grid = grid;
        this.grid.setParent(this);
    }

    /**
     * The time interval is the number of discrete timesteps that will be
     * discarded when storing the resulting solution into a {@code HeatingCurve}
     * object, thus ensuring that only a limited set of points is stored.
     *
     * @return the time interval
     */
    public final int getTimeInterval() {
        return timeInterval;
    }

    /**
     * Sets the time interval to the argument of this method.
     *
     * @param timeInterval a positive integer.
     */
    public final void setTimeInterval(int timeInterval) {
        this.timeInterval = timeInterval;
    }

    /**
     * If true, Lets the UI know that the user only wants to have the most
     * important properties displayed. Otherwise this will signal all properties
     * need to be displayed.
     */
    @Override
    public final boolean areDetailsHidden() {
        return hideDetailedAdjustment;
    }

    /**
     * Changes the policy of displaying a detailed information about this
     * scheme.
     *
     * @param b a boolean.
     */
    public final static void setDetailsHidden(boolean b) {
        hideDetailedAdjustment = b;
    }

    /**
     * The time limit (in whatever units this {@code DifferenceScheme} uses to
     * process the solution), which serves as the ultimate breakpoint for the
     * calculations.
     *
     * @return the {@code NumericProperty} with the type {@code TIME_LIMIT}
     * @see pulse.properties.NumericPropertyKeyword
     */
    public final NumericProperty getTimeLimit() {
        return derive(TIME_LIMIT, timeLimit);
    }

    public double getCurrentPulseValue() {
        return pls;
    }

    /**
     * Sets the time limit (in units defined by the corresponding
     * {@code NumericProperty}), which serves as the breakpoint for the
     * calculations.
     *
     * @param timeLimit the {@code NumericProperty} with the type
     * {@code TIME_LIMIT}
     * @see pulse.properties.NumericPropertyKeyword
     */
    public final void setTimeLimit(NumericProperty timeLimit) {
        requireType(timeLimit, TIME_LIMIT);
        this.timeLimit = (double) timeLimit.getValue();
        firePropertyChanged(this, timeLimit);
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        if (type == TIME_LIMIT) {
            setTimeLimit(property);
        }
    }
    
    public abstract double signal();
    
    public abstract void clearArrays();

    public abstract void timeStep(int m) throws SolverException;

    public abstract void finaliseStep() throws SolverException;

        /**
     * Retrieves all problem statements that can be solved with this
     * implementation of the difference scheme.
     *
     * @return an array containing subclasses of the {@code Problem} class which
     * can be used as input for this difference scheme.
     */
    public abstract Class<? extends Problem>[] domain();

    /**
     * Creates a {@code DifferenceScheme}, which is an exact copy of this
     * object.
     *
     * @return an exact copy of this {@code DifferenceScheme}.
     */
    public abstract DifferenceScheme copy();
    
}