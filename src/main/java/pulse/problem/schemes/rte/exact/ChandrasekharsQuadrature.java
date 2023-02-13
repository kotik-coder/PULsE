package pulse.problem.schemes.rte.exact;

import static java.lang.Math.PI;
import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.exp;
import static java.lang.Math.sqrt;
import static pulse.math.MathUtils.fastPowLoop;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.QUADRATURE_POINTS;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.commons.math3.analysis.solvers.LaguerreSolver;

import pulse.math.Segment;
import pulse.math.linear.Matrices;
import pulse.math.linear.SquareMatrix;
import pulse.math.linear.Vector;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;

/**
 * This quadrature methods of evaluating the composition product of the
 * exponential integral and blackbody spectral power spectrum has been given by
 * Chandrasekhar and is based on constructing a moment matrix.
 *
 * @see <a href="https://archive.org/details/RadiativeTransfer">Chandrasekhar,
 * S. Radiative transfer</a>
 *
 */
public class ChandrasekharsQuadrature extends CompositionProduct {

    private static final long serialVersionUID = 3282258803373408111L;
    private int m;
    private double expLower;
    private double expUpper;
    private transient LaguerreSolver solver;
    private double[] moments;

    /**
     * Constructs a {@code ChandrasekharsQuadrature} object with a default
     * number of nodes, a {@code LaguerreSolver} with default precision and
     * integration bounds set to [0,1].
     */
    public ChandrasekharsQuadrature() {
        super(new Segment(0, 1));
        m = (int) def(QUADRATURE_POINTS).getValue();
        solver = new LaguerreSolver();
    }

    @Override
    public double integrate() {
        var bounds = this.transformedBounds();
        expLower = -exp(-bounds[0]);
        expUpper = -exp(-bounds[1]);

        double[] roots = roots();

        Vector weights = weights(roots);

        return f(roots).dot(weights) / getBeta();
    }

    public NumericProperty getQuadraturePoints() {
        return derive(QUADRATURE_POINTS, m);
    }

    public void setQuadraturePoints(NumericProperty m) {
        requireType(m, QUADRATURE_POINTS);
        this.m = (int) m.getValue();
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        if (type == QUADRATURE_POINTS) {
            setQuadraturePoints(property);
            firePropertyChanged(this, property);
        }
    }

    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(QUADRATURE_POINTS);
        return set;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " : " + getQuadraturePoints();
    }

    /*
	 * Private methods
     */
    private Vector f(final double[] roots) {
        final var ef = getEmissionFunction();
        return new Vector(Arrays.stream(roots).map(root -> ef.powerAt(root)).toArray());
    }

    private double[] transformedBounds() {
        final double min = getBounds().getMinimum();
        final double max = getBounds().getMaximum();
        return new double[]{getAlpha() + getBeta() * min, getAlpha() + getBeta() * max};
    }

    private SquareMatrix xMatrix(final double[] roots) {
        double[][] x = new double[m][m];

        for (int l = 0; l < m; l++) {
            for (int j = 0; j < m; j++) {
                x[l][j] = fastPowLoop(roots[j] * getBeta() + getAlpha(), l);
            }
        }

        return Matrices.createSquareMatrix(x);
    }

    /**
     * Calculates \int_{r_{min}}^{r_{max}}{x^{l+1}exp(-x)dx}.
     *
     * @param l an integer such that 0 <= l <= 2*m - 1. @re
     * turn the value of this definite integral.
     */
    private static double auxilliaryIntegral(final double x, final int lPlusN, final double exp) {

        double f = 0;
        long m = 0;

        final int k = lPlusN - 1;

        for (int i = 0; i < lPlusN; i++) {
            m = 1;
            for (int j = 0; j < i; j++) {
                m *= (k - j);
            }
            f += m * fastPowLoop(x, k - i);
        }

        return f * exp;

    }

    private static double[] solveCubic(final double a, final double b, final double c) {
        final double p = b / 3.0 - a * a / 9.0;
        final double q = a * a * a / 27.0 - a * b / 6.0 + c / 2.0;

        final double ang = acos(-q / sqrt(-p * p * p));
        final double r = 2.0 * sqrt(-p);
        var result = new double[3];
        double theta;
        for (int k = -1; k < 2; k++) {
            theta = (ang - 2.0 * PI * k) / 3.0;
            result[k + 1] = r * cos(theta);
        }

        for (int i = 0; i < result.length; i++) {
            result[i] -= a / 3.0;
        }

        return result;
    }

    private double moment(int l) {
        var bounds = this.transformedBounds();
        return momentIntegral(bounds[1], l, expUpper) - momentIntegral(bounds[0], l, expLower);
    }

    private double momentIntegral(final double x, final int l, final double exp) {

        double e = 0;
        int m = 0;

        final int n = getOrder();
        final int lPlusOne = l + 1;

        for (int i = 0; i < n; i++) {
            m = lPlusOne;
            for (int j = 1; j < i + 1; j++) {
                m *= (lPlusOne + j);
            }
            e += ExponentialIntegrals.get(n - i).valueAt(x) * fastPowLoop(x, lPlusOne + i) / ((double) m);
        }

        return e + auxilliaryIntegral(x, l + n, exp) / ((double) m);

    }

    private Vector coefficients() {
        return momentMatrix().inverse().multiply(momentVector(m, 2 * m));
    }

    private SquareMatrix momentMatrix() {

        double[][] data = new double[m][m];
        moments = new double[2 * m];

        // diagonal elements
        IntStream.range(0, m).forEach(i -> data[i][i] = moment(i * 2));

        // find (symmetric) non-diagonal elements
        for (int i = 1, j = 0; i < m; i++) {
            for (j = 0; j < i; j++) {
                data[i][j] = moment(i + j);
                data[j][i] = data[i][j];
            }
        }

        for (int i = 0; i < m; i++) {
            moments[i] = data[0][i];
            moments[i + m - 1] = data[i][m - 1];
        }

        moments[2 * m - 1] = moment(2 * m - 1);

        return Matrices.createSquareMatrix(data);

    }

    private Vector momentVector(final int lowerInclusive, final int upperExclusive) {
        var array = IntStream.range(lowerInclusive, upperExclusive).mapToDouble(i -> -moments[i]).toArray();
        return new Vector(array);
    }

    private Vector weights(final double[] roots) {
        final var x = xMatrix(roots);
        final var a = momentVector(0, m).inverted();

        return x.inverse().multiply(a);
    }

    private double[] roots() {
        double[] roots;
        double[] c = new double[m + 1];

        // coefficients of the monic polynomial x_j^m + sum_{l=0}^{m-1}{c_lx_j^l}
        System.arraycopy(coefficients().getData(), 0, c, 0, m);
        c[m] = 1.0;

        switch (m) {
            // m = 1 never used
            case 2:
                roots = new double[2];
                // solve quadratic equation, all roots of which are real
                final double det = sqrt(c[1] * c[1] - 4.0 * c[0]);
                roots[0] = (-c[1] + det) * 0.5;
                roots[1] = (-c[1] - det) * 0.5;
                break;
            case 3:
                roots = new double[3];
                // solve cubic equation, all roots of which are real
                roots = solveCubic(c[2], c[1], c[0]);
                break;
            default:
                // use LaguerreSolver
                if (solver == null) {
                    solver = new LaguerreSolver();
                }
                roots = Arrays.stream(solver.solveAllComplex(c, 1.0)).mapToDouble(complex -> complex.getReal()).toArray();
        }

        for (int i = 0; i < roots.length; i++) {
            roots[i] = (roots[i] - getAlpha()) / getBeta();
        }

        return roots;

    }

}
