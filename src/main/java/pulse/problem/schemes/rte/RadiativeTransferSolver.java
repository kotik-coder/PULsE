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

	private double[] fluxes;
	private double[] storedFluxes;

	private int N;
	private double h;
	private double opticalThickness;
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
		reinitFluxes((int) grid.getGridDensity().getValue());
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
	 * Resets the flux arrays by erasing all their contents and resizing to {@code N}.
	 * @param N the new size of flux arrays
	 */

	protected void reinitFluxes(int N) {
		this.N = N;
		this.h = opticalThickness / N;
		fluxes = new double[N + 1];
		storedFluxes = new double[N + 1];
	}
	
	/**
	 * Retrieves the parameters from {@code p} and {@code grid} needed to run the calculations.
	 * Resets the flux arrays.
	 * @param p the problem statement
	 * @param grid the grid
	 */

	public void init(ParticipatingMedium p, Grid grid) {
		this.opticalThickness = (double) p.getOpticalThickness().getValue();
		reinitFluxes((int) grid.getGridDensity().getValue());
	}

	/**
	 * Performs interpolation with natural cubic splines using the input arguments.
	 * 
	 * @param tempArray an array of data defined on a previously initialised grid.
	 * @return a {@code UnivariateFunction} generated with a {@code SplineInterpolator}
	 */

	public UnivariateFunction interpolateTemperatureProfile(double[] tempArray) {
		double[] xArray = new double[tempArray.length];

		for (int i = 0; i < xArray.length; i++) 
			xArray[i] = opticalCoordinateAt(i);

		return (new SplineInterpolator()).interpolate(xArray, tempArray);
	}
	
	/**
	 * Calculates the average value of the flux derivatives at the {@code uIndex} grid point
	 * on the current and previous timesteps. 
	 * @param uIndex the grid point index
	 * @return the time-averaged value of the flux derivative at {@code uIndex}
	 */

	public abstract double meanFluxDerivative(int uIndex);
	
	/**
	 * Calculates the average value of the flux derivatives at the first grid point
	 * on the current and previous timesteps. 
	 * @return the time-averaged value of the flux derivative at the front surface
	 */
	
	public abstract double meanFluxDerivativeFront();
	
	/**
	 * Calculates the average value of the flux derivatives at the last grid point
	 * on the current and previous timesteps. 
	 * @return the time-averaged value of the flux derivative at the rear surface
	 */
	
	public abstract double meanFluxDerivativeveRear(); 
	
	/**
	 * Calculates the flux derivative at the {@code uIndex} grid point.
	 * @param uIndex the grid point index
	 * @return the value of the flux derivative at {@code uIndex}
	 */
	
	public abstract double fluxDerivative(int uIndex); 
	
	/**
	 * Calculates the flux derivative at the front surface.
	 * @return the value of the flux derivative at the front surface
	 */
	
	public abstract double fluxDerivativeFront();
	
	/**
	 * Calculates the flux derivative at the rear surface.
	 * @return the value of the flux derivative at the rear surface
	 */
	
	public abstract double fluxDerivativeRear();
	
	/**
	 * Retrieves the currently calculated flux at the {@code i} grid point
	 * @param i the index of the grid point
	 * @return the flux value at the specified grid point
	 */
	
	public double getFlux(int i) {
		return fluxes[i];
	}
	
	/**
	 * Sets the flux at the {@code i} grid point
	 * @param i the index of the grid point
	 */

	public void setFlux(int i, double value) {
		this.fluxes[i] = value;
	}
	
	/**
	 * Retrieves the previously calculated flux at the {@code i} grid point.
	 * @param i the index of the grid point
	 * @return the previous flux value at the specified grid point
	 * @see store()
	 */

	public double getStoredFlux(int i) {
		return storedFluxes[i];
	}
	
	/**
	 * Retrieves the grid density of the heat problem.
	 * @return the grid density of the heat problem
	 */

	public int getExternalGridDensity() {
		return N;
	}
	
	/**
	 * Retrieves the optical coordinate corresponding to the grid index {@code i}
	 * @param i the external grid index
	 * @return <math>&tau;<sub>0</sub>/<i>N</i> <i>i</i> </math>
	 */

	public double opticalCoordinateAt(int i) {
		return h * i;
	}
	
	/**
	 * Stores all currently calculated fluxes in a separate array. 
	 */

	public void store() {
		System.arraycopy(fluxes, 0, storedFluxes, 0, N + 1); // store previous results
	}
	
	/**
	 * Retrieves the grid step multiplied by &tau;<sub>0</sub>.
	 * @return the resized grid step
	 */

	public double getOpticalGridStep() {
		return h;
	}
	
	/**
	 * Retrieves the &tau;<sub>0</sub> value.
	 * @return the optical thickness value.
	 */

	public double getOpticalThickness() {
		return opticalThickness;
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

	public void addRTEListener(RTECalculationListener listener) {
		rteListeners.add(listener);
	}

}