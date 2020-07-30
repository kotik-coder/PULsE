package pulse.problem.schemes.rte.exact;

import static java.lang.Math.PI;
import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.exp;
import static java.lang.Math.sqrt;
import static pulse.math.MathUtils.fastPowLoop;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericProperty.theDefault;
import static pulse.properties.NumericPropertyKeyword.QUADRATURE_POINTS;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.solvers.LaguerreSolver;

import pulse.math.Matrix;
import pulse.math.Segment;
import pulse.math.Vector;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public class ChandrasekharsQuadrature extends CompositionProduct {

	private int m;
	private double expLower;
	private double expUpper;
	private LaguerreSolver solver;
	private double[] moments;

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
		return NumericProperty.derive(NumericPropertyKeyword.QUADRATURE_POINTS, m);
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
		List<Property> list = new ArrayList<>();
		list.add(NumericProperty.def(NumericPropertyKeyword.QUADRATURE_POINTS));
		return list;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " : " + getQuadraturePoints();
	}

	/*
	 * Private methods
	 */

	private Vector f(double[] roots) {
		double f[] = new double[roots.length];

		for (int i = 0; i < f.length; i++)
			f[i] = getEmissionFunction().powerAt(roots[i]);

		return new Vector(f);

	}

	private double transformedBound(double x) {
		return getAlpha() + getBeta() * x;
	}

	private double transformedMaximum() {
		return transformedBound(getBounds().getMaximum());
	}

	private double transformedMinimum() {
		return transformedBound(getBounds().getMinimum());
	}

	private Matrix xMatrix(int m, double[] roots) {
		double[][] x = new double[m][m];

		for (int l = 0; l < m; l++) {
			for (int j = 0; j < m; j++) {
				x[l][j] = fastPowLoop(roots[j] * getBeta() + getAlpha(), l);
			}
		}

		return new Matrix(x);
	}

	/**
	 * Calculates \int_{r_{min}}^{r_{max}}{x^{l+1}exp(-x)dx}.
	 * 
	 * @param l an integer such that 0 <= l <= 2*m - 1.
	 * @return the value of this definite integral.
	 */

	private static double auxilliaryIntegral(double x, int l, int n, double exp) {

		double f = 0;
		long m = 0;

		int k = l + n - 1;

		for (int i = 0, j = 0; i < k + 1; i++) {
			m = 1;
			for (j = 0; j < i; j++) {
				m *= (k - j);
			}
			f += m * fastPowLoop(x, k - i);
		}

		return f * exp;

	}

	private static double[] solveCubic(double a, double b, double c) {
		double[] result;

		double p = b / 3 - a * a / 9;
		double q = a * a * a / 27 - a * b / 6 + c / 2;

		double ang = acos(-q / sqrt(-p * p * p));
		double r = 2 * sqrt(-p);
		result = new double[3];
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

	private double momentIntegral(double x, int l, int n, double exp) {

		double e = 0;
		int m = 0;

		int lPlusOne = l + 1;

		for (int i = 0, j = 0; i < n; i++) {
			m = lPlusOne;
			for (j = 1; j < i + 1; j++) {
				m *= (lPlusOne + j);
			}
			e += ExponentialIntegrals.get(n - i).valueAt(x) * fastPowLoop(x, lPlusOne + i) / m;
		}

		return e + auxilliaryIntegral(x, l, n, exp) / m;

	}

	private Vector coefficients(int m, int n) {
		return momentMatrix(m, n).inverse().multiply(momentVector(m, 2 * m));
	}

	private Matrix momentMatrix(int m, int n) {

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

		return new Matrix(data);

	}

	private Vector momentVector(int lowerInclusive, int upperExclusive) {
		Vector v = new Vector(upperExclusive - lowerInclusive);

		for (int i = lowerInclusive; i < upperExclusive; i++) {
			v.set(i - lowerInclusive, -moments[i]);
		}

		return v;

	}

	private Vector weights(int m, double[] roots) {
		var x = xMatrix(m, roots);
		var a = momentVector(0, m).inverted();

		return x.inverse().multiply(a);
	}

	private double[] roots(int m, int n) {
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