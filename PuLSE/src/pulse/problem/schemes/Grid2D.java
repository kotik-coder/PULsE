package pulse.problem.schemes;

import static java.lang.Math.pow;
import pulse.properties.NumericProperty;

public class Grid2D extends Grid {

	protected double hy;
	
	public Grid2D(NumericProperty gridDensity, NumericProperty timeFactor) {
		super(gridDensity, timeFactor);
	}
	
	public Grid2D copy() {
		return new Grid2D(getGridDensity(), getTimeFactor());
	}
	
	@Override
	public void setTimeFactor(NumericProperty timeFactor) {
		super.setTimeFactor(timeFactor);
		tau	= tauFactor*( pow(hx, 2) + pow( hy, 2) );
	}
	
	@Override
	public void setGridDensity(NumericProperty gridDensity) {
		super.setGridDensity(gridDensity);
		hy = hx;
	}
	
	/**
	 * The dimensionless time on a grid, which is the {@code time/timeFactor} rounded up to a factor of the time step {@code tau}.
	 * @param problem the problem containing a reference to {@code Pulse}
	 * @return a double representing the time on the finite grid
	 * @see Pulse
	 */
	
	public double gridRadialDistance(double radial, double lengthFactor) {
		return Math.rint( (radial/lengthFactor)/hy )*hy;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("Grid2D: <math><i>h<sub>x</sub></i>=" + String.format("%3.3f", hx) + "; ");
		sb.append("<math><i>h<sub>y</sub></i>=" + String.format("%3.3f", hy) + "; ");
		sb.append("<i>&tau;</i>="+String.format("%3.4f", tau));
		return sb.toString();
	}
	
	
}