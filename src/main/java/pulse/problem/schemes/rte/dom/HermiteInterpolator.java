package pulse.problem.schemes.rte.dom;

/**
 * A globally C<sup>1</sup> Hermite interpolator used to interpolate intensities
 * and derivatives in discrete ordinates method when solving the radiative
 * transfer equation with a Runge-Kutta solver (either implicit or explicit).
 *
 * @author Vadim Zborovskii, Artem Lunev
 *
 */
public class HermiteInterpolator {

    protected double y1;
    protected double y0;
    protected double d1;
    protected double d0;

    protected double a;
    protected double bMinusA;

    public void clear() {
        y1 = 0;
        y0 = 0;
        d1 = 0;
        d0 = 0;
        a = 0;
        bMinusA = 0;
    }

    /**
     * Interpolates the function at {@code x}
     *
     * @param x the value within the specified bounds
     * @return the interpolated value
     */
    public double interpolate(final double x) {
        final double t = (x - a) / bMinusA;
        final double tMinusOne = (t - 1.0);
        return t * t * (3.0 - 2.0 * t) * y1 + tMinusOne * tMinusOne * (1.0 + 2.0 * t) * y0
                + (t * t * tMinusOne * d1 + tMinusOne * tMinusOne * t * d0) * bMinusA;
    }

    /**
     * Calculates the derivative of the interpolant at {@code x}
     *
     * @param x the value within the specified bounds
     * @return the derivative of the interpolant
     */
    public double derivative(final double x) {
        final double t = (x - a) / bMinusA;
        final double tt1 = t * (t - 1.0);

        return 6.0 * tt1 * (y0 - y1) / bMinusA + (t * (3.0 * t - 2.0) * d1 + (3.0 * t * t - 4.0 * t + 1.0) * d0);
    }

    @Override
    public String toString() {
        return String.format("%n (%3.6f ; %3.6f), (%3.6f ; %3.6f), (%3.6f, %3.6f)", y0, y1, d0, d1, a, (bMinusA - a));
    }

    /**
     * Interpolates intensities and their derivatives w.r.t. tau on EXTERNAL
     * grid points of the heat problem solver based on the derivatives on
     * INTERNAL grid points of DOM solver.
     *
     * @param externalGridSize the number of points in the external grid
     * @param integrator the adaptive integrator
     * @return a three-dimensional array containing the interpolated intensities
     * and derivatives
     */
    public double[][][] interpolateOnExternalGrid(final int externalGridSize, AdaptiveIntegrator integrator) {
        final var discrete = integrator.getDiscretisation();
        final var intensities = discrete.getQuantities().getIntensities();
        final var internalGrid = discrete.getGrid();
        final var derivatives = discrete.getQuantities().getDerivatives();
        final int total = discrete.getOrdinates().getTotalNodes();

        var iExt = new double[2][externalGridSize][total];
        double t;

        final double hx = internalGrid.getDimension() / (externalGridSize - 1.0);
        final int n = internalGrid.getDensity() + 1;

        /*
		 * Loop through the external grid points
         */
        outer:
        for (int i = 0; i < iExt[0].length; i++) {
            t = i * hx;

            // loops through nodes sorted in ascending order
            for (int j = 0; j < n; j++) {

                /*
				 * if node is greater than t, then the associated function can be interpolated
				 * between points f_i and f_i-1, since t lies between nodes n_i and n_i-1
                 */
                if (internalGrid.getNode(j) > t) { // nearest points on internal grid have been found -> j - 1 and j

                    a = internalGrid.getNode(j - 1);
                    bMinusA = internalGrid.stepLeft(j);

                    /*
					 * Loops through ordinate set
                     */
                    for (int k = 0; k < total; k++) {
                        y0 = intensities[j - 1][k];
                        y1 = intensities[j][k];
                        d0 = derivatives[j - 1][k];
                        d1 = derivatives[j][k];
                        iExt[0][i][k] = interpolate(t); // intensity
                        iExt[1][i][k] = derivative(t); // derivative
                    }

                    continue outer; // move to next point t

                }

            }

            for (int k = 0; k < total; k++) {
                iExt[0][i][k] = intensities[internalGrid.getDensity()][k];
                iExt[1][i][k] = derivatives[internalGrid.getDensity()][k];
            }

        }

        return iExt;
    }

}
