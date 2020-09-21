package pulse.problem.schemes;

import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.TIME_LIMIT;

import java.util.ArrayList;
import java.util.List;

import pulse.problem.laser.DiscretePulse;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
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

	/**
	 * Used to get a class of problems on which this difference scheme is
	 * applicable.
	 * 
	 * @return a subclass of the {@code Problem} class which can be used as input
	 *         for this difference scheme.
	 */

	public abstract Class<? extends Problem> domain();

	/**
	 * Creates a {@code DifferenceScheme}, which is an exact copy of this object.
	 * 
	 * @return an exact copy of this {@code DifferenceScheme}.
	 */

	public abstract DifferenceScheme copy();

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
	 * Contains preparatory steps to ensure smooth running of the solver. This
	 * includes creating a {@code DiscretePulse} object and calculating the
	 * {@code timeInterval}. The latter determines the real-time calculation of a
	 * {@code HeatingCurve} based on the numerical solution of {@code problem}; it
	 * thus takes into account the difference between the scheme timestep and the
	 * {@code HeatingCurve} point spacing. All subclasses of
	 * {@code DifferenceScheme} should override and explicitly call this superclass
	 * method where appropriate.
	 * </p>
	 * 
	 * @param problem the heat problem to be solved
	 */

	protected void prepare(Problem problem) {
		discretePulse = problem.discretePulseOn(grid);
		grid.adjustTo(discretePulse);

		var hc = problem.getHeatingCurve();
		hc.clear();
	}

	public void runTimeSequence(Problem problem) {
		runTimeSequence(problem, 0, timeLimit);
		var curve = problem.getHeatingCurve();
		final double maxTemp = (double) problem.getProperties().getMaximumTemperature().getValue();
		curve.scale(maxTemp / curve.apparentMaximum() );
	}

	public void runTimeSequence(Problem problem, final double offset, final double endTime) {
		final var grid = getGrid();

		var curve = problem.getHeatingCurve();

		int adjustedNumPoints = (int)curve.getNumPoints().getValue();
		
		final double startTime = (double)curve.getTimeShift().getValue();
		final double timeSegment = (endTime - startTime - offset) / problem.getProperties().timeFactor();
		final double tau = grid.getTimeStep();
		
		for (double dt = 0, factor = 1.0; dt < tau; adjustedNumPoints *= factor) {
			dt = timeSegment / (adjustedNumPoints - 1);
			factor = dt / tau;
			timeInterval = (int) factor;
		}

		final double wFactor = timeInterval * tau * problem.getProperties().timeFactor();

		// First point (index = 0) is always (0.0, 0.0)

		/*
		 * The outer cycle iterates over the number of points of the HeatingCurve
		 */

		double nextTime = offset + wFactor + startTime;
		curve.addPoint(startTime, 0.0); 
				
		for (int w = 1; nextTime < 1.01*endTime; nextTime = offset + startTime + (++w)*wFactor) {

			/*
			 * Two adjacent points of the heating curves are separated by timeInterval on
			 * the time grid. Thus, to calculate the next point on the heating curve,
			 * timeInterval/tau time steps have to be made first.
			 */

			timeSegment((w - 1) * timeInterval + 1, w * timeInterval + 1);
			curve.addPoint(nextTime, signal()); 
	
		}
		
	}

	private void timeSegment(final int m1, final int m2) {
		for (int m = m1; m < m2 && normalOperation(); m++) {
			timeStep(m);
			finaliseStep();
		}
	}

	public double pulse(final int m) {
		return getDiscretePulse().laserPowerAt((m - EPS) * getGrid().getTimeStep());
	}

	public abstract double signal();

	public abstract void timeStep(final int m);

	public abstract void finaliseStep();

	public boolean normalOperation() {
		return true;
	}

	/**
	 * The superclass only lists the {@code TIME_LIMIT} property.
	 */

	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<>();
		list.add(def(TIME_LIMIT));
		return list;
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

	public DiscretePulse getDiscretePulse() {
		return discretePulse;
	}

	/**
	 * Gets the {@code Grid} object defining partioning used in this
	 * {@code DifferenceScheme}
	 * 
	 * @return the grid
	 */

	public Grid getGrid() {
		return grid;
	}

	/**
	 * Sets the grid and adopts it as its child.
	 * 
	 * @param grid the grid
	 */

	public void setGrid(Grid grid) {
		this.grid = grid;
		this.grid.setParent(this);
	}

	/**
	 * The time interval is the number of discrete timesteps that will be discarded
	 * when storing the resulting solution into a {@code HeatingCurve} object, thus
	 * ensuring that only a limited set of points is stored.
	 * 
	 * @return the time interval
	 */

	public int getTimeInterval() {
		return timeInterval;
	}

	/**
	 * Sets the time interval to the argument of this method.
	 * 
	 * @param timeInterval a positive integer.
	 */

	public void setTimeInterval(int timeInterval) {
		this.timeInterval = timeInterval;
	}

	/**
	 * If true, Lets the UI know that the user only wants to have the most important
	 * properties displayed. Otherwise this will signal all properties need to be
	 * displayed.
	 */

	@Override
	public boolean areDetailsHidden() {
		return hideDetailedAdjustment;
	}

	/**
	 * Changes the policy of displaying a detailed information about this scheme.
	 * 
	 * @param b a boolean.
	 */

	public static void setDetailsHidden(boolean b) {
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

	public NumericProperty getTimeLimit() {
		return derive(TIME_LIMIT, timeLimit);
	}

	/**
	 * Sets the time limit (in units defined by the corresponding
	 * {@code NumericProperty}), which serves as the breakpoint for the
	 * calculations.
	 * 
	 * @param timeLimit the {@code NumericProperty} with the type {@code TIME_LIMIT}
	 * @see pulse.properties.NumericPropertyKeyword
	 */

	public void setTimeLimit(NumericProperty timeLimit) {
		requireType(timeLimit, TIME_LIMIT);
		this.timeLimit = (double) timeLimit.getValue();
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		if (type == TIME_LIMIT)
			setTimeLimit(property);
	}

}