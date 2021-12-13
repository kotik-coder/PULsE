package pulse.problem.schemes.rte.dom;

import pulse.math.LegendrePoly;
import pulse.math.MathUtils;

/**
 * A composite Gaussian quadrature for numerical evaluation of the scattering
 * integral in one-dimensional heat transfer.
 *
 * @author Teymur Aliev, Vadim Zborovskii, Artem Lunev
 *
 */
public class CompositeGaussianQuadrature {

    private LegendrePoly poly;

    private double[] roots;

    private double[] nodes;
    private double[] weights;

    private int n;

    /**
     * Constructs a composite Gaussian quadrature for an even {@code n}
     *
     * @param n an even integer
     */
    public CompositeGaussianQuadrature(final int n) {
        if (n % 2 != 0) {
            throw new IllegalArgumentException(n + " is odd. Even number expected.");
        }
        this.n = n;
        poly = new LegendrePoly(n / 2);
        roots = poly.roots();
        nodes();
        weights();
    }

    private void nodes() {

        nodes = new double[n];
        weights = new double[n];

        for (int i = 0; i < n / 2; i++) {

            nodes[i] = 0.5 * (1.0 + roots[i]);
            nodes[i + n / 2] = -0.5 * (1.0 + roots[i]);

        }

    }

    /**
     * Calculates the Gaussian weights. Uses the formula by Abramowitz & Stegun
     * (Abramowitz & Stegun 1972, p. 887))
     */
    private void weights() {
        double denominator = 1;

        for (int i = 0; i < n / 2; i++) {
            denominator = (1 - roots[i] * roots[i]) * MathUtils.fastPowLoop(poly.derivative(roots[i]), 2);
            weights[i] = 1.0 / denominator;
            weights[i + n / 2] = weights[i];
        }

    }

    /**
     * The weights of the composite quadrature.
     *
     * @return the weights
     */
    public double[] getWeights() {
        return weights;
    }

    /**
     * The cosine nodes of the composite quadrature.
     *
     * @return the cosine nodes
     */
    public double[] getNodes() {
        return nodes;
    }

}
