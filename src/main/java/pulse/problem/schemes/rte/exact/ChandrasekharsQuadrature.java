package pulse.problem.schemes.rte.exact;

import static java.lang.Math.PI;
import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.exp;
import static java.lang.Math.sqrt;
import static pulse.math.MathUtils.fastPowLoop;
import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericProperty.theDefault;
import static pulse.properties.NumericPropertyKeyword.QUADRATURE_POINTS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.analysis.solvers.LaguerreSolver;

import pulse.math.Segment;
import pulse.math.linear.Matrices;
import pulse.math.linear.SquareMatrix;
import pulse.math.linear.Vector;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

/**
 * This quadrature methods of evaluating the composition product of the exponential integral and 
 * blackbody spectral power spectrum has been given by Chandrasekhar and is based
 * on constructing a moment matrix.
 * @see <a href="Chandrasekhar, S. Radiative transfer.">shorturl.at/de179</a> 
 * 
 */

public class ChandrasekharsQuadrature extends CompositionProduct {

	private int m;
	private double expLower;
	private double expUpper;
	private LaguerreSolver solver;
	private double[] moments;

	/**
	 * Constructs a {@code ChandrasekharsQuadrature} object with a default 
	 * number of nodes, a {@code LaguerreSolver} with default precision and 
	 * integration bounds set to [0,1].
	 */
	
	public ChandrasekharsQuadrature() {
		super(new Segment(0, 1));
		m = (int) theDefault(QUADRATURE_POINTS).getValue();
		solver = new LaguerreSolver();
	}
	
	@Override
	public double integrate() {
		expLower = -exp(-transformedMinimum());
		expUpper = -exp(-transformedMaximum());

		double[] roots = roots(m, getOrder());

		Vector weights = weights(m, roots);

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
	public List<Property> listedTypes() {
		return new ArrayList<Property>(Arrays.asList(def(QUADRATURE_POINTS)));
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " : " + getQuadraturePoints();
	}

	/*
	 * Private methods
	 */

	private Vector f(final double[] roots) {
		double f[] = new double[roots.length];
		final var ef = getEmissionFunction();

		for (int i = 0; i < f.length; i++)
			f[i] = ef.powerAt(roots[i]);

		return new Vector(f);
	}

	private double transformedBound(final double x) {
		return getAlpha() + getBeta() * x;
	}

	private double transformedMaximum() {
		return transformedBound(getBounds().getMaximum());
	}

	private double transformedMinimum() {
		return transformedBound(getBounds().getMinimum());
	}

	private SquareMatrix xMatrix(final int m, final double[] roots) {
		double[][] x = new double[m][m];
		
		for (int l = 0; l < m; l++) {
			for (int j = 0; j < m; j++) {
				x[l][j] = fastPowLoop(roots[j] * getBeta() + getAlpha(), l);
			}
		}

		return Matrices.createMatrix(x);
	}

	/**
	 * Calculates \int_{r_{min}}^{r_{max}}{x^{l+1}exp(-x)dx}.
	 * 
	 * @param l an integer such that 0 <= l <= 2*m - 1.
	 * @return the value of this definite integral.
	 */

	private static double auxilliaryIntegral(final double x, final int l, final int n, final double exp) {

		double f = 0;
		long m = 0;

		final int k = l + n - 1;

		for (int i = 0, j = 0; i < k + 1; i++) {
			m = 1;
			for (j = 0; j < i; j++) {
				m *= (k - j);
			}
			f += m * fastPowLoop(x, k - i);
		}

		return f * exp;

	}

	private static double[] solveCubic(final double a, final double b, final double c) {
		final double p = b / 3 - a * a / 9;
		final double q = a * a * a / 27 - a * b / 6 + c / 2;

		final double ang = acos(-q / sqrt(-p * p * p));
		final double r = 2 * sqrt(-p);
		var result = new double[3];
		double theta;
		for (int k = -1; k <= 1; k++) {
			theta = (ang - 2 * PI * k) / 3;
			result[k + 1] = r * cos(theta);
		}

		for (int i = 0; i < result.length; i++) {
			result[i] -= a / 3;
		}

		return result;
	}

	private double moment(int l, int n) {
		return momentIntegral(transformedMaximum(), l, n, expUpper)
				- momentIntegral(transformedMinimum(), l, n, expLower);
	}

	private double momentIntegral(final double x, final int l, final int n, final double exp) {

		double e = 0;
		int m = 0;

		final int lPlusOne = l + 1;

		for (int i = 0, j = 0; i < n; i++) {
			m = lPlusOne;
			for (j = 1; j < i + 1; j++) {
				m *= (lPlusOne + j);
			}
			e += ExponentialIntegrals.get(n - i).valueAt(x) * fastPowLoop(x, lPlusOne + i) / m;
		}

		return e + auxilliaryIntegral(x, l, n, exp) / m;

	}

	private Vector coefficients(final int m, final int n) {
		return momentMatrix(m, n).inverse().multiply(momentVector(m, 2 * m));
	}

	private SquareMatrix momentMatrix(final int m, final int n) {

		double[][] data = new double[m][m];
		moments = new double[2 * m];

		// find diagonal elements
		for (int i = 0; i < m; i++) {
			data[i][i] = moment(i * 2, n);
		}

		// find (symmetric) non-diagonal elements
		for (int i = 1, j = 0; i < m; i++) {
			for (j = 0; j < i; j++) {
				data[i][j] = moment(i + j, n);
				data[j][i] = data[i][j];
			}
		}

		for (int i = 0; i < m; i++) {
			moments[i] = data[0][i];
			moments[i + m - 1] = data[i][m - 1];
		}

		moments[2 * m - 1] = moment(2 * m - 1, n);

		return Matrices.createMatrix(data);

	}

	private Vector momentVector(final int lowerInclusive, final int upperExclusive) {
		Vector v = new Vector(upperExclusive - lowerInclusive);

		for (int i = lowerInclusive; i < upperExclusive; i++) {
			v.set(i - lowerInclusive, -moments[i]);
		}

		return v;

	}

	private Vector weights(final int m, final double[] roots) {
		final var x = xMatrix(m, roots);
		final var a = momentVector(0, m).inverted();

		return x.inverse().multiply(a);
	}

	private double[] roots(final int m, final int n) {
		double[] roots = new double[m];
		double[] c = new double[m + 1];

		// coefficients of the monic polynomial x_j^m + sum_{l=0}^{m-1}{c_lx_j^l}
		System.arraycopy(coefficients(m, n).getData(), 0, c, 0, m);
		c[m] = 1.0;

		switch (m) {
		// m = 1 never used
		case 2:
			roots[0] = (-c[1] + sqrt(c[1] * c[1] - 4.0 * c[0])) * 0.5;
			roots[1] = (-c[1] - sqrt(c[1] * c[1] - 4.0 * c[0])) * 0.5;
			break;
		case 3:
			roots = solveCubic(c[2], c[1], c[0]);
			break;
		default:
			var complexRoots = solver.solveAllComplex(c, 1.0);

			for (int i = 0; i < complexRoots.length; i++) {
				roots[i] = complexRoots[i].getReal();
			}
		}

		for (int i = 0; i < roots.length; i++) {
			roots[i] = (roots[i] - getAlpha()) / getBeta();
		}

		return roots;

	}

}