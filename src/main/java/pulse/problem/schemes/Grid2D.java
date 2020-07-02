package pulse.problem.schemes;

import static java.lang.Math.pow;
import static java.lang.Math.rint;
import static java.lang.String.format;
import static pulse.properties.NumericPropertyKeyword.SPOT_DIAMETER;

import pulse.problem.laser.DiscretePulse;
import pulse.problem.laser.DiscretePulse2D;
import pulse.properties.NumericProperty;

/**
 * <p>
 * A {@code Grid2D} is used to partition the space and time domain of a
 * {@code Problem2D} to allow a numeric solution with a
 * {@code DifferenceScheme}. This type of grid is two-dimensional in space,
 * meaning that it defines rules for partitioning of both the axial and radial
 * dimensions for interpreting the laser flash experiments.
 * </p>
 */

public class Grid2D extends Grid {

	protected double hy;

	protected Grid2D() {
		super();
	}

	public Grid2D(NumericProperty gridDensity, NumericProperty timeFactor) {
		super(gridDensity, timeFactor);
	}

	@Override
	public Grid2D copy() {
		return new Grid2D(getGridDensity(), getTimeFactor());
	}

	@Override
	public void setTimeFactor(NumericProperty timeFactor) {
		super.setTimeFactor(timeFactor);
		tau = tauFactor * (pow(hx, 2) + pow(hy, 2));
	}
	
	/**
	 * Calls the {@code optimise} method from superclass, then adjusts the
	 * {@code gridDensity} of the {@code grid} if
	 * {@code discretePulseSpot < (Grid2D)grid.hy}.
	 * 
	 * @param grid an instance of {@code Grid2D}
	 */

	@Override
	public void adjustTo(DiscretePulse pulse) {
		super.adjustTo(pulse);
		if(pulse instanceof DiscretePulse2D)
			optimise((DiscretePulse2D)pulse);
	}
	
	public void optimise(DiscretePulse2D pulse) {
		for (final var factor = 1.05; factor * hy > pulse.getDiscretePulseSpot(); pulse.recalculate(SPOT_DIAMETER)) {
			N += 5;
			hy = 1. / N;
			hx = 1. / N;
		}
		
	}
	
	/**
	 * Sets the value of the {@code gridDensity}. Automatically recalculates the
	 * {@code hx} an {@code hy} values.
	 */

	@Override
	public void setGridDensity(NumericProperty gridDensity) {
		super.setGridDensity(gridDensity);
		hy = hx;
	}

	/**
	 * The dimensionless radial distance on this {@code Grid2D}, which is the
	 * {@code radial/lengthFactor} rounded up to a factor of the coordinate step
	 * {@code hy}.
	 * 
	 * @param radial       the distance along the radial direction
	 * @param lengthFactor a factor which has the dimension of length
	 * @return a double representing the radial distance on the finite grid
	 */

	public double gridRadialDistance(double radial, double lengthFactor) {
		return rint((radial / lengthFactor) / hy) * hy;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append("<html>");
		sb.append("Grid2D: <math><i>h<sub>x</sub></i>=" + format("%3.3f", hx) + "; ");
		sb.append("<math><i>h<sub>y</sub></i>=" + format("%3.3f", hy) + "; ");
		sb.append("<i>&tau;</i>=" + format("%3.4f", tau));
		return sb.toString();
	}

	public double getYStep() {
		return hy;
	}

}