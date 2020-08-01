package pulse.problem.schemes.rte;

public abstract class Fluxes implements DerivativeCalculator {

	private int N;
	private double opticalThickness;
	private double[] fluxes;
	private double[] storedFluxes;

	public Fluxes(int N, double tau0) {
		setOpticalThickness(tau0);
		setDensity(N);
	}
	
	public void setDensity(int N) {
		this.N = N;
		fluxes = new double[N + 1];
		storedFluxes = new double[N + 1];
	}
	
	/**
	 * Stores all currently calculated fluxes in a separate array.
	 */

	public void store() {
		System.arraycopy(fluxes, 0, storedFluxes, 0, N + 1); // store previous results
	}
	
	/**
	 * Retrieves the currently calculated flux at the {@code i} grid point
	 * 
	 * @param i the index of the grid point
	 * @return the flux value at the specified grid point
	 */

	public double getFlux(int i) {
		return fluxes[i];
	}

	/**
	 * Sets the flux at the {@code i} grid point
	 * 
	 * @param i the index of the grid point
	 */

	public void setFlux(int i, double value) {
		this.fluxes[i] = value;
	}

	/**
	 * Retrieves the previously calculated flux at the {@code i} grid point.
	 * 
	 * @param i the index of the grid point
	 * @return the previous flux value at the specified grid point
	 * @see store()
	 */

	public double getStoredFlux(int i) {
		return storedFluxes[i];
	}
	
	public int getDensity() {
		return N;
	}
	
	public double getOpticalGridStep() { 
		return opticalThickness/((double)N);
	}

	public double getOpticalThickness() {
		return opticalThickness;
	}

	public void setOpticalThickness(double opticalThickness) {
		this.opticalThickness = opticalThickness;
	}
	
}