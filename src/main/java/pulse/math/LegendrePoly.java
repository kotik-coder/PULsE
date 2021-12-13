package pulse.math;

import static pulse.math.MathUtils.fastPowInt;
import static pulse.math.MathUtils.fastPowLoop;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericPropertyKeyword.LAGUERRE_SOLVER_ERROR;

import org.apache.commons.math3.analysis.solvers.LaguerreSolver;

/**
 * A utility class represents the Legendre polynomial of a given order.
 * <p>
 * May be useful in some applications, particularly, for calculating the
 * ordinate sets in the discrete ordinates method. Allows calculating its
 * coefficients, the value of the polynomial and its derivative at a given
 * point, and the roots of the polynomial (with the help of a
 * {@code LaguerreSolver}.
 * </p>
 *
 * @see org.apache.commons.math3.analysis.solvers.LaguerreSolver
 * @see <a href="https://en.wikipedia.org/wiki/Legendre_polynomials">Wiki
 * page</a>
 */
public class LegendrePoly {

    private double[] c;
    private int n;

    private LaguerreSolver solver;

    /**
     * Creates a Legendre polynomial of the order {@code n}. The coefficients of
     * the polynomial are immediately calculated.
     *
     * @param n the order of the polynomial.
     */
    public LegendrePoly(final int n) {
        this.n = n;
        c = new double[n + 1];
        var solverError = (double) def(LAGUERRE_SOLVER_ERROR).getValue();
        solver = new LaguerreSolver(solverError);
        coefficients();
    }

    /**
     * Fast calculation of binomial coefficient C<sub>m</sub><sup>k</sup> =
     * m!/(k!(m-k)!)
     *
     * @param m integer.
     * @param k integer, k &le; m.
     * @return binomial coefficient C<sub>m</sub><sup>k</sup>.
     */
    public static long binomial(int m, int k) {
        int k1 = m - k;
        k = k > k1 ? k1 : k;

        long c = 1;

        // Calculate value of Binomial Coefficient in bottom up manner
        for (int i = 1; i < k + 1; i++, m--) {
            c = c / i * m + c % i * m / i; // split c * n / i into (c / i * i + c % i) * n / i
        }

        return c;

    }

    /**
     * Calculates the generalised binomial coefficient.
     *
     * @param k integer.
     * @param alpha a double value
     * @return the generalised binomial coefficient
     * C<sub>&alpha;</sub><sup>k</sup>.
     */
    public static double generalisedBinomial(final double alpha, final int k) {

        double c = 1;

        for (int i = 0; i < k; i++) {
            c *= (alpha - i) / (k - i);
        }

        return c;

    }

    /**
     * This will generate the coefficients for the Legendre polynomial, arranged
     * in order of significance (from x<sup>0</sup> to x<sup>n</sup>). The
     * coeffients will then be stored in a double array for further use.
     */
    public void coefficients() {

        long intFactor = fastPowInt(2, n);

        for (int i = 0; i < c.length; i++) {
            c[i] = intFactor * binomial(n, i) * generalisedBinomial((n + i - 1) * 0.5, n);
        }

    }

    /**
     * Calculates the derivative of this Legendre polynomial. The coefficients
     * are assumed to be known.
     *
     * @param x a real value.
     * @return the derivative at {@code x}.
     */
    public double derivative(final double x) {
        double d = 0;

        for (int i = 1; i < c.length; i++) {
            d += i * c[i] * fastPowLoop(x, i - 1);
        }

        return d;

    }

    /**
     * @return the order of this polynomial.
     */
    public int getOrder() {
        return n;
    }

    /**
     * Calculates the value of this Legendre polynomial at {@code x}
     *
     * @param x a real value.
     * @return the value of the Legendre polynomial at {@code x}.
     */
    public double poly(final double x) {
        double poly = 0;

        for (int i = 0; i < c.length; i++) {
            poly += c[i] * fastPowLoop(x, i);
        }

        return poly;

    }

    /**
     * Uses a {@code LaguerreSolver} to calculate the roots of this polynomial.
     * All coefficients are assumed to be known.
     *
     * @return the real roots of the Legendre polynomial.
     */
    public double[] roots() {
        var complexRoots = solver.solveAllComplex(c, 1.0);
        var roots = new double[n];

        // the last roots is always zero, so we have n non-zero roots in total
        // in case of even n, the first n/2 roots are positive and the rest are negative
        for (int i = 0; i < n; i++) {
            roots[i] = complexRoots[i].getReal();
        }

        return roots;

    }

}
