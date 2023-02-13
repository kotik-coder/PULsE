package pulse.problem.schemes.rte;

import java.io.Serializable;

/**
 * This is basically a coupling interface between a {@code Solver} and a
 * {@code RadiativeTransferSolver}.
 *
 */
public interface DerivativeCalculator extends Serializable {

    /**
     * Calculates the average value of the flux derivatives at the
     * {@code uIndex} grid point on the current and previous timesteps.
     *
     * @param uIndex the grid point index
     * @return the time-averaged value of the flux derivative at {@code uIndex}
     */
    public double meanFluxDerivative(int uIndex);

    /**
     * Calculates the average value of the flux derivatives at the first grid
     * point on the current and previous timesteps.
     *
     * @return the time-averaged value of the flux derivative at the front
     * surface
     */
    public double meanFluxDerivativeFront();

    /**
     * Calculates the average value of the flux derivatives at the last grid
     * point on the current and previous timesteps.
     *
     * @return the time-averaged value of the flux derivative at the rear
     * surface
     */
    public double meanFluxDerivativeRear();

    /**
     * Calculates the flux derivative at the {@code uIndex} grid point.
     *
     * @param uIndex the grid point index
     * @return the value of the flux derivative at {@code uIndex}
     */
    public double fluxDerivative(int uIndex);

    /**
     * Calculates the flux derivative at the front surface.
     *
     * @return the value of the flux derivative at the front surface
     */
    public double fluxDerivativeFront();

    /**
     * Calculates the flux derivative at the rear surface.
     *
     * @return the value of the flux derivative at the rear surface
     */
    public double fluxDerivativeRear();

}
