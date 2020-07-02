package pulse.problem.schemes;

import static java.lang.Math.pow;
import static java.lang.Math.rint;
import static java.lang.String.format;
import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericPropertyKeyword.GRID_DENSITY;
import static pulse.properties.NumericPropertyKeyword.PULSE_WIDTH;
import static pulse.properties.NumericPropertyKeyword.TAU_FACTOR;

import java.util.ArrayList;
import java.util.List;

import pulse.problem.laser.DiscretePulse;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;

/**
 * <p>
 * A {@code Grid} is used to partition the space and time domain of a
 * {@code Problem} to allow a numeric solution with a {@code DifferenceScheme}.
 * This type of grid is one-dimensional in space, meaning that it only defines
 * rules for partitioning the axial dimension in the laser flash experiment.
 * </p>
 *
 */

public class Grid extends PropertyHolder {

	protected double hx;
	protected double tau;
	protected double tauFactor;
	protected int N;

	/**
	 * Creates a {@code Grid} object with the specified {@code gridDensity} and
	 * {@code timeFactor}.
	 * 
	 * @param gridDensity a {@code NumericProperty} of the type {@code GRID_DENSITY}
	 * @param timeFactor  a {@code NumericProperty} of the type {@code TIME_FACTOR}
	 * @see pulse.properties.NumericPropertyKeyword
	 */

	public Grid(NumericProperty gridDensity, NumericProperty timeFactor) {
		setGridDensity(gridDensity);
		setTimeFactor(timeFactor);
	}

	protected Grid() { }

	/**
	 * Creates a new {@code Grid} object with exactly the same parameters as this
	 * one.
	 * 
	 * @return a new {@code Grid} object replicating this {@code Grid}
	 */

	public Grid copy() {
		return new Grid(getGridDensity(), getTimeFactor());
	}
	
	/**
	 * Optimises the {@code grid} parameters.
	 * <p>
	 * This can change the {@code tauFactor} and {@code tau} variables in the
	 * {@code grid} object if {@code discretePulseWidth < grid.tau}.
	 * </p>
	 * 
	 * @param grid the grid to be adjusted
	 */

	public void adjustTo(DiscretePulse pulse) {
		for (final var factor = 0.95; factor * tau > pulse.getDiscretePulseWidth(); pulse.recalculate(PULSE_WIDTH)) {
			tauFactor /= 1.5;
			tau = tauFactor * pow(hx, 2);
		}
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<>(2);
		list.add(def(GRID_DENSITY));
		list.add(def(TAU_FACTOR));
		return list;
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch (type) {
		case TAU_FACTOR:
			setTimeFactor(property);
			break;
		case GRID_DENSITY:
			setGridDensity(property);
			break;
		default:
			break;
		}
	}

	/**
	 * Retrieves the value of the <math><i>h<sub>x</sub></i></math> coordinate step
	 * used in finite-difference calculation.
	 * 
	 * @return a double, representing the {@code hx} value.
	 */

	public double getXStep() {
		return hx;
	}

	/**
	 * Sets the value of the <math><i>h<sub>x</sub></i></math> coordinate step.
	 * 
	 * @param hx a double, representing the new {@code hx} value.
	 */

	public void setXStep(double hx) {
		this.hx = hx;
	}

	/**
	 * Retrieves the value of the &tau; time step used in finite-difference
	 * calculation.
	 * 
	 * @return a double, representing the {@code tau} value.
	 */

	public double getTimeStep() {
		return tau;
	}

	/**
	 * Retrieves the value of the &tau;-factor, or the time factor, used in
	 * finite-difference calculation. This factor determines the proportionally
	 * coefficient between &tau; and <math><i>h<sub>x</sub></i></math>.
	 * 
	 * @return a NumericProperty of the {@code TAU_FACTOR} type, representing the
	 *         {@code tauFactor} value.
	 */

	public NumericProperty getTimeFactor() {
		return derive(TAU_FACTOR, tauFactor);
	}

	/**
	 * Retrieves the value of the {@code gridDensity} used to calculate the
	 * {@code hx} and {@code tau}.
	 * 
	 * @return a NumericProperty of the {@code GRID_DENSITY} type, representing the
	 *         {@code gridDensity} value.
	 */

	public NumericProperty getGridDensity() {
		return derive(GRID_DENSITY, this.N);
	}

	/**
	 * Sets the value of the {@code gridDensity}. Automatically recalculates the
	 * {@code hx} value.
	 * 
	 * @param gridDensity a NumericProperty of the {@code GRID_DENSITY} type
	 */

	public void setGridDensity(NumericProperty gridDensity) {
		if (gridDensity.getType() != GRID_DENSITY)
			throw new IllegalArgumentException("Type of property passed to constructor " + gridDensity.getType());
		this.N = (int) gridDensity.getValue();
		hx = 1. / N;
		setTimeFactor(derive(TAU_FACTOR, 1.0));
	}

	/**
	 * Sets the value of the {@code tauFactor}. Automatically recalculates the
	 * {@code tau} value.
	 * 
	 * @param timeFactor a NumericProperty of the {@code TAU_FACTOR} type
	 */

	public void setTimeFactor(NumericProperty timeFactor) {
		if (timeFactor.getType() != TAU_FACTOR)
			throw new IllegalArgumentException("Wrong NumericProperty type passed to method: " + timeFactor.getType());

		this.tauFactor = (double) timeFactor.getValue();
		tau = tauFactor * pow(hx, 2);
	}

	/**
	 * The dimensionless time on this {@code Grid}, which is the
	 * {@code time/timeFactor} rounded up to a factor of the time step {@code tau}.
	 * 
	 * @param time            the time
	 * @param dimensionFactor a factor which has the dimension of time
	 * @return a double representing the time on the finite grid
	 * @see pulse.problem.statements.Problem.timeFactor()
	 */

	public double gridTime(double time, double dimensionFactor) {
		return rint((time / dimensionFactor) / tau) * tau;
	}

	/**
	 * The dimensionless axial distance on this {@code Grid}, which is the
	 * {@code distance/lengthFactor} rounded up to a factor of the coordinate step
	 * {@code hx}.
	 * 
	 * @param distance     the distance along the axial direction
	 * @param lengthFactor a factor which has the dimension of length
	 * @return a double representing the axial distance on the finite grid
	 */

	public double gridAxialDistance(double distance, double lengthFactor) {
		return rint((distance / lengthFactor) / hx) * hx;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append("<html>");
		sb.append("Grid: <math><i>h<sub>x</sub></i>=" + format("%3.2e", hx) + "; ");
		sb.append("<i>&tau;</i>=" + format("%3.2e", tau));
		return sb.toString();
	}

}