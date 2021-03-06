package pulse.problem.laser;

import static java.lang.Math.signum;

import pulse.problem.schemes.Grid2D;
import pulse.problem.statements.ClassicalProblem2D;
import pulse.problem.statements.Pulse2D;
import pulse.problem.statements.model.ExtendedThermalProperties;

/**
 * The discrete pulse on a {@code Grid2D}.
 * <p>
 * The main parameters are the {@code discretePulseWidth} (defined in the
 * superclass) and {@code discretePulseSpot}, which is the discretised version
 * of the spot diameter of the respective {@code Pulse} object.
 * </p>
 *
 */

public class DiscretePulse2D extends DiscretePulse {

	private double discretePulseSpot;
	private double coordFactor;

	/**
	 * The constructor for {@code DiscretePulse2D}.
	 * <p>
	 * Calls the constructor of the superclass, after which calculates the
	 * {@code discretePulseSpot} using the {@code gridRadialDistance} method of this
	 * class. The dimension factor is defined as the sample diameter.
	 * </p>
	 * 
	 * @param problem a two-dimensional problem
	 * @param grid    the two-dimensional grid
	 */

	public DiscretePulse2D(ClassicalProblem2D problem, Grid2D grid) {
		super(problem, grid);
		var properties = (ExtendedThermalProperties)problem.getProperties();
		coordFactor = (double) properties.getSampleDiameter().getValue() / 2.0;
		var pulse = (Pulse2D)problem.getPulse();
		discretePulseSpot = grid.gridRadialDistance((double) pulse.getSpotDiameter().getValue() / 2.0, coordFactor);

	}

	/**
	 * This calculates the dimensionless, discretised pulse function at a
	 * dimensionless radial coordinate {@code coord}.
	 * <p>
	 * It uses a Heaviside function to determine whether the {@code radialCoord}
	 * lies within the {@code 0 <= radialCoord <= discretePulseSpot} interval. It
	 * uses the {@code time} parameter to determine the discrete pulse function
	 * using {@code evaluateAt(time)}.
	 * 
	 * @param time        the time for calculation
	 * @param radialCoord - the radial coordinate [length dimension]
	 * @return the pulse function at {@code time} and {@code coord}, or 0 if
	 *         {@code coord > spotDiameter}.
	 * @see pulse.problem.laser.PulseTemporalShape.laserPowerAt(double)
	 */

	public double evaluateAt(double time, double radialCoord) {
		return laserPowerAt(time) * (0.5 + 0.5 * signum(discretePulseSpot - radialCoord));
	}

	/**
	 * Calls the superclass method, then calculates the {@code discretePulseSpot}
	 * using the {@code gridRadialDistance} method.
	 * 
	 * @see pulse.problem.schemes.Grid2D.gridRadialDistance(double,double)
	 */

	@Override
	public void recalculate() {
		super.recalculate();
		final double radius = (double) ((Pulse2D) getPulse()).getSpotDiameter().getValue() / 2.0;
		discretePulseSpot = ((Grid2D) getGrid()).gridRadialDistance(radius, coordFactor);
	}

	public double getDiscretePulseSpot() {
		return discretePulseSpot;
	}

}