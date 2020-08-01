package pulse.problem.schemes.rte;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;

import pulse.problem.schemes.Grid;
import pulse.problem.statements.ParticipatingMedium;
import pulse.util.Descriptive;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

/**
 * An abstract class defining the radiative fluxes and their derivatives for use
 * within the coupled conductive-radiative problem solver. Uses a
 * {@code SplineInterpolator} to generate a smooth spatial temperature profile.
 * Provides means of probing the calculation health and tracking calculation
 * steps with listeners.
 *
 */

public abstract class RadiativeTransferSolver extends PropertyHolder implements Reflexive, Descriptive {

	private Fluxes fluxes;
	private List<RTECalculationListener> rteListeners;

	/**
	 * Abstract constructor that initialises flux arrays based on the {@code grid}
	 * density. The reason why flux arrays should have the same dimension as the
	 * grid number of elements is because they will be used from within the heat
	 * problem solver, which in turn requires the fluxes to be defined at
	 * {@code Grid} points.
	 * 
	 * @param grid the {@code Grid} object specifying the number of elements in the
	 *             flux arrays.
	 */

	public RadiativeTransferSolver(ParticipatingMedium problem, Grid grid) {
		rteListeners = new ArrayList<>();
	}

	/**
	 * Launches a calculation of the radiative transfer equation.
	 * 
	 * @param temperatureArray
	 * @return the status of calculation
	 */

	public abstract RTECalculationStatus compute(double[] temperatureArray);

	/**
	 * Retrieves the parameters from {@code p} and {@code grid} needed to run the
	 * calculations. Resets the flux arrays.
	 * 
	 * @param p    the problem statement
	 * @param grid the grid
	 */

	public void init(ParticipatingMedium p, Grid grid) {
		final double opticalThickness = (double) p.getOpticalThickness().getValue();
		if (fluxes != null) {
			fluxes.setDensity((int) grid.getGridDensity().getValue());
			fluxes.setOpticalThickness(opticalThickness);
		}
	}

	/**
	 * Performs interpolation with natural cubic splines using the input arguments.
	 * 
	 * @param tempArray an array of data defined on a previously initialised grid.
	 * @return a {@code UnivariateFunction} generated with a
	 *         {@code SplineInterpolator}
	 */

	public UnivariateFunction interpolateTemperatureProfile(double[] tempArray) {
		double[] xArray = new double[tempArray.length];

		for (int i = 0; i < xArray.length; i++)
			xArray[i] = opticalCoordinateAt(i);

		return (new SplineInterpolator()).interpolate(xArray, tempArray);
	}

	/**
	 * Retrieves the optical coordinate corresponding to the grid index {@code i}
	 * 
	 * @param i the external grid index
	 * @return <math>&tau;<sub>0</sub>/<i>N</i> <i>i</i> </math>
	 */

	public double opticalCoordinateAt(int i) {
		return fluxes.getOpticalGridStep() * i;
	}

	@Override
	public boolean ignoreSiblings() {
		return true;
	}

	@Override
	public String getPrefix() {
		return "Radiative Transfer Solver";
	}

	public List<RTECalculationListener> getRTEListeners() {
		return rteListeners;
	}

	/**
	 * Adds a listener that can listen to status updates.
	 * 
	 * @param listener a listener to track the calculation progress
	 */

	public void addRTEListener(RTECalculationListener listener) {
		rteListeners.add(listener);
	}

	public void fireStatusUpdate(RTECalculationStatus status) {
		for (RTECalculationListener l : getRTEListeners())
			l.onStatusUpdate(status);
	}

	public Fluxes getFluxes() {
		return fluxes;
	}

	public void setFluxes(Fluxes fluxes) {
		this.fluxes = fluxes;
	}

}