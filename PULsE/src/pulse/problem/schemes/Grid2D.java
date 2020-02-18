package pulse.problem.schemes;

import static java.lang.Math.pow;

import pulse.properties.NumericProperty;

/**
 * <p>A {@code Grid2D} is used to partition the space and time domain of a {@code Problem2D}
 * to allow a numeric solution with a {@code DifferenceScheme}. This type of grid
 * is two-dimensional in space, meaning that it defines rules for partitioning 
 * of both the axial and radial dimensions for interpreting the laser flash experiments.</p>
 */

public class Grid2D extends Grid {

	protected double hy;
	
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
		tau	= tauFactor*( pow(hx, 2) + pow( hy, 2) );
	}
	
	/**
	 * Sets the value of the {@code gridDensity}. Automatically recalculates the {@code hx} an {@code hy} values.  
	 */
	
	@Override
	public void setGridDensity(NumericProperty gridDensity) {
		super.setGridDensity(gridDensity);
		hy = hx;
	}
	
	/**
	 * The dimensionless radial distance on this {@code Grid2D}, which is the {@code radial/lengthFactor} rounded up to a factor of the coordinate step {@code hy}.
	 * @param radial the distance along the radial direction
	 * @param lengthFactor a factor which has the dimension of length
	 * @return a double representing the radial distance on the finite grid
	 */
	
	public double gridRadialDistance(double radial, double lengthFactor) {
		return Math.rint( (radial/lengthFactor)/hy )*hy;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("Grid2D: <math><i>h<sub>x</sub></i>=" + String.format("%3.3f", hx) + "; ");
		sb.append("<math><i>h<sub>y</sub></i>=" + String.format("%3.3f", hy) + "; ");
		sb.append("<i>&tau;</i>="+String.format("%3.4f", tau));
		return sb.toString();
	}
	
}