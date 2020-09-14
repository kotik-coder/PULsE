package pulse.problem.laser;

import pulse.problem.schemes.Grid;
import pulse.problem.statements.Problem;
import pulse.problem.statements.Pulse;

/**
 * A {@code DiscretePulse} is an object that acts as a medium between the
 * physical {@code Pulse} and the respective {@code DifferenceScheme} used to
 * process the solution of a {@code Problem}.
 * 
 * @see pulse.problem.statements.Pulse
 */

public class DiscretePulse {

	private Grid grid;
	private Pulse pulse;
	private double discretePulseWidth;
	private double timeFactor;

	/**
	 * This creates a one-dimensional discrete pulse on a {@code grid}.
	 * <p>
	 * The dimensional factor is taken from the {@code problem}, while the discrete
	 * pulse width (a multiplier of the {@code grid} parameter {@code tau} is
	 * calculated using the {@code gridTime} method.
	 * </p>
	 * 
	 * @param problem the problem, used to extract the dimensional time factor
	 * @param grid    a grid used to discretise the {@code pulse}
	 */

	public DiscretePulse(Problem problem, Grid grid) {
		timeFactor = problem.getProperties().timeFactor();
		this.grid = grid;
		this.pulse = problem.getPulse();

		recalculate();

		pulse.getPulseShape().init(this);
		pulse.addListener(e -> {
			recalculate();
			pulse.getPulseShape().init(this);

		});
	}

	/**
	 * Uses the {@code PulseTemporalShape} of the {@code Pulse} object to calculate
	 * the laser power at the specified moment of {@code time}.
	 * 
	 * @param time the time argument
	 * @return the laser power at the specified moment of {@code time}
	 */

	public double laserPowerAt(double time) {
		return pulse.getPulseShape().evaluateAt(time);
	}

	/**
	 * Recalculates the {@code discretePulseWidth} by calling {@code gridTime} on
	 * the physical pulse width and {@code timeFactor}.
	 * 
	 * @see pulse.problem.schemes.Grid.gridTime(double,double)
	 */

	public void recalculate() {
		final double width = ((Number) pulse.getPulseWidth().getValue()).doubleValue();
		discretePulseWidth = grid.gridTime(width, timeFactor);
	}

	/**
	 * Gets the discrete pulse width defined by {@code DiscretePulse}.
	 * 
	 * @return a double, representing the discrete pulse width.
	 */

	public double getDiscreteWidth() {
		return discretePulseWidth;
	}

	/**
	 * Gets the physical {@code Pulse}
	 * 
	 * @return the {@code Pulse} object
	 */

	public Pulse getPulse() {
		return pulse;
	}

	/**
	 * Gets the {@code Grid} object used to construct this {@code DiscretePulse}
	 * 
	 * @return the {@code Grid} object.
	 */

	public Grid getGrid() {
		return grid;
	}

}