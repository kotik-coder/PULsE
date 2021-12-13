package pulse.problem.schemes.rte;

import pulse.properties.NumericProperty;

public abstract class Fluxes implements DerivativeCalculator {

    private int N;
    private double opticalThickness;
    private double[] fluxes;
    private double[] storedFluxes;

    public Fluxes(NumericProperty gridDensity, NumericProperty opticalThickness) {
        setOpticalThickness(opticalThickness);
        setDensity(gridDensity);
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

    public double getOpticalGridStep() {
        return opticalThickness / ((double) N);
    }

    public int getDensity() {
        return N;
    }

    public double getOpticalThickness() {
        return opticalThickness;
    }

    public void setDensity(NumericProperty gridDensity) {
        this.N = (int) gridDensity.getValue();
        fluxes = new double[N + 1];
        storedFluxes = new double[N + 1];
    }

    public void setOpticalThickness(NumericProperty opticalThickness) {
        this.opticalThickness = (double) opticalThickness.getValue();
    }

}
