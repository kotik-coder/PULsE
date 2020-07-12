package pulse.problem.schemes;

import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericProperty.derive;
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
 * A {@code DifferenceScheme} is defined as a sequence of algebraic operations
 * on a {@code Grid} that is used to calculate the solution of a
 * {@code Problem}.
 * <p>
 * The difference scheme thus replaces the partial derivatives in the
 * formulation of the heat problem statement and uses approximate methods to
 * calculate the solution. It relies on a {@code Grid} object, which defines the
 * time and coordinate partitioning, adjusted to ensure a stable or
 * conditionally-stable behavior of the solution. The {@code Grid} is also used
 * to define a {@code DiscretePulse} function.
 * </p>
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
	 * Subclasses use this constructor to create a {@code DifferenceScheme} on a
	 * custom {@code Grid} specified by the {@code N} and {@code timeFactor}
	 * parameters.
	 * <p>
	 * The parent of the {@code Grid} object should be set to this
	 * {@code DifferenceScheme}. Time limit for calculation is set to default value
	 * (as specified in the XML file).
	 * </p>
	 * 
	 * @param N          a {@code NumericProperty} with the {@code GRID_DENSITY}
	 *                   type
	 * @param timeFactor a {@code NumericProperty} with the {@code TIME_FACTOR} type
	 * @see pulse.properties.NumericPropertyKeyword
	 * @see pulse.problem.schemes.Grid(NumericProperty,NumericProperty)
	 */

	protected DifferenceScheme() {
		setTimeLimit(def(TIME_LIMIT));
	}

	/**
	 * Constructs a {@code DifferenceScheme} with a custom {@code Grid} specified by
	 * the {@code N} and {@code timeFactor} parameters, and also using a custom time
	 * limit.
	 * <p>
	 * The parent of the {@code Grid} object is set to this
	 * {@code DifferenceScheme}.
	 * 
	 * @param N          a {@code NumericProperty} with the {@code GRID_DENSITY}
	 *                   type
	 * @param timeFactor a {@code NumericProperty} with the {@code TIME_FACTOR} type
	 * @see pulse.properties.NumericPropertyKeyword
	 * @see pulse.problem.schemes.Grid(NumericProperty,NumericProperty)
	 */

	protected DifferenceScheme(NumericProperty timeLimit) {
		setTimeLimit(timeLimit);
	}

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
	 * Calculates the solution of {@code problem} using in-built algebraic
	 * relations.
	 * <p>
	 * This specific implementation of the {@code solve} method only contains
	 * preparatory steps to ensure smooth running of the more concrete
	 * implementations. These steps include creating a {@code DiscretePulse} object
	 * and calculating the {@code timeInterval}. The latter determines the real-time
	 * calculation of a {@code HeatingCurve} based on the numerical solution of
	 * {@code problem}; it thus takes into account the difference between the scheme
	 * timestep and the {@code HeatingCurve} data spacing. All subclasses of
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
		setTimeInterval( (int) (dt / grid.tau) + 1 );

		hc.reinit();
	}

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

	public void setGrid(Grid grid) {
		this.grid = grid;
		this.grid.setParent(this);
	}

	public void initGrid(NumericProperty N, NumericProperty timeFactor) {
		setGrid(new Grid(N, timeFactor));
	}

	public int getTimeInterval() {
		return timeInterval;
	}

	public void setTimeInterval(int timeInterval) {
		this.timeInterval = timeInterval;
	}
	
	@Override
	public boolean areDetailsHidden() {
		return hideDetailedAdjustment;
	}

	public static void setDetailsHidden(boolean b) {
		hideDetailedAdjustment = b;
	}
	
	/**
	 * The time limit (in whatever units this {@code DifferenceScheme} uses to
	 * process the solution), which serves as the breakpoint for the calculations.
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
		if (timeLimit.getType() != TIME_LIMIT)
			throw new IllegalArgumentException("Illegal property type : " + timeLimit.getType());
		this.timeLimit = (double) timeLimit.getValue();
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch (type) {
		case TIME_LIMIT:
			setTimeLimit(property);
			break;
		default:
			throw new IllegalArgumentException("Property not recognised: " + property);
		}
	}

}