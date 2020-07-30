package pulse.problem.schemes;

import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericProperty.derive;
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

		final var numPoints = (int) hc.getNumPoints().getValue();
		final var dt = timeLimit / (problem.timeFactor() * (numPoints - 1));
		setTimeInterval((int) (dt / grid.getTimeStep()) + 1);

		hc.reinit();
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