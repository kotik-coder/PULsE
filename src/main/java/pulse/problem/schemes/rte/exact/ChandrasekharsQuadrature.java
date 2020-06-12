package pulse.problem.schemes.rte.exact;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.solvers.LaguerreSolver;

import pulse.algebra.Matrix;
import pulse.algebra.Vector;
import pulse.problem.schemes.rte.MathUtils;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public class ChandrasekharsQuadrature extends SimpsonsRule {

	private int m;
	private double expLower, expUpper;
	private LaguerreSolver solver;
	private final double PRECISION = 1E-3;

	private double[] moments;

	private final static double DEFAULT_CUTOFF = 20.0;

	public ChandrasekharsQuadrature() {
		super();
		this.cutoff = DEFAULT_CUTOFF;
		m = (int) NumericProperty.theDefault(NumericPropertyKeyword.QUADRATURE_POINTS).getValue();
		this.integrationSegments = m - 1;
		solver = new LaguerreSolver(PRECISION);
	}

	private Vector f(int n, double[] roots, double alpha, double beta) {
		double f[] = new double[roots.length];

		double tdim;
		int floor;

		for (int i = 0; i < f.length; i++) {
			tdim = roots[i] / tau0;
			floor = (int) (tdim / hx); // floor index
			alpha = tdim / hx - floor;
			f[i] = emissionFunction.power((1.0 - alpha) * U[floor] + alpha * U[floor + 1]);
		}

		return new Vector(f);

	}

	@Override
	public void adjustRange(double alpha, double beta) {
		super.adjustRange(alpha, beta);
		rMin = alpha + beta * rMin;
		rMax = alpha + beta * rMax;
	}

	@Override
	public double integrate(int n, double... params) {

		adjustRange(params[A_INDEX], params[B_INDEX]);

		expLower = -Math.exp(-rMin);
		expUpper = -Math.exp(-rMax);

		double[] roots = roots(m, n, params[A_INDEX], params[B_INDEX]);

		Vector weights = weights(m, n, roots, params[A_INDEX], params[B_INDEX]);

		return f(n, roots, params[A_INDEX], params[B_INDEX]).dot(weights) / params[B_INDEX];

	}

	public Vector weights(int m, int n, double[] roots, double alpha, double beta) {

		var x = xMatrix(m, n, roots, alpha, beta);

		var a = momentVector(0, m, n).inverted();

		return x.inverse().multiply(a);

	}

	public Matrix xMatrix(int m, int n, double[] roots, double alpha, double beta) {

		double[][] x = new double[m][m];

		for (int l = 0; l < m; l++)
			for (int j = 0; j < m; j++)
				x[l][j] = MathUtils.fastPowLoop(roots[j] * beta + alpha, l);

		return new Matrix(x);

	}

	public double[] roots(int m, int n, double alpha, double beta) {

		double[] roots = new double[m];

		double[] c = new double[m + 1];

		// coefficients of the monic polynomial x_j^m + sum_{l=0}^{m-1}{c_lx_j^l}
		System.arraycopy(coefficients(m, n).getData(), 0, c, 0, m);
		c[m] = 1.0;

		switch (m) {
		case 2: // m = 1 never used
			roots[0] = (-c[1] + Math.sqrt(c[1] * c[1] - 4.0 * c[0])) * 0.5;
			roots[1] = (-c[1] - Math.sqrt(c[1] * c[1] - 4.0 * c[0])) * 0.5;
			break;
		case 3:
			roots = solveCubic(c[2], c[1], c[0]);
			break;
		default:
			var complexRoots = solver.solveAllComplex(c, 1.0);

			for (int i = 0; i < complexRoots.length; i++)
				roots[i] = complexRoots[i].getReal();
		}

		for (int i = 0; i < roots.length; i++)
			roots[i] = (roots[i] - alpha) / beta;

		return roots;

	}

	private static double[] solveCubic(double a, double b, double c) {
		double[] result;

		double p = b / 3 - a * a / 9;
		double q = a * a * a / 27 - a * b / 6 + c / 2;

		double ang = Math.acos(-q / Math.sqrt(-p * p * p));
		double r = 2 * Math.sqrt(-p);
		result = new double[3];
		double theta;
		for (int k = -1; k <= 1; k++) {
			theta = (ang - 2 * Math.PI * k) / 3;
			result[k + 1] = r * Math.cos(theta);
		}

		for (int i = 0; i < result.length; i++)
			result[i] = result[i] - a / 3;

		return result;
	}

	public Vector coefficients(int m, int n) {

		return momentMatrix(m, n).inverse().multiply(momentVector(m, 2 * m, n));

	}

	public Matrix momentMatrix(int m, int n) {

		double[][] data = new double[m][m];
		moments = new double[2 * m];

		// find diagonal elements
		for (int i = 0; i < m; i++)
			data[i][i] = moment(i * 2, n);

		// find (symmetric) non-diagonal elements
		for (int i = 1, j = 0; i < m; i++)
			for (j = 0; j < i; j++) {
				data[i][j] = moment(i + j, n);
				data[j][i] = data[i][j];
			}

		for (int i = 0; i < m; i++) {
			moments[i] = data[0][i];
			moments[i + m - 1] = data[i][m - 1];
		}

		moments[2 * m - 1] = moment(2 * m - 1, n);

		return new Matrix(data);

	}

	public Vector momentVector(int lowerInclusive, int upperExclusive, int n) {
		Vector v = new Vector(upperExclusive - lowerInclusive);

		for (int i = lowerInclusive; i < upperExclusive; i++)
			v.set(i - lowerInclusive, -moments[i]);

		return v;

	}

	private double moment(int l, int n) {
		return momentIntegral(rMax, l, n, expUpper) - momentIntegral(rMin, l, n, expLower);
	}

	private double momentIntegral(double x, int l, int n, double exp) {

		double e = 0;
		int m = 0;

		int lPlusOne = l + 1;

		for (int i = 0, j = 0; i < n; i++) {
			m = lPlusOne;
			for (j = 1; j < i + 1; j++)
				m *= (lPlusOne + j);
			e += expIntegrator.integralAt(x, n - i) * MathUtils.fastPowLoop(x, lPlusOne + i) / m;
		}

		return e + auxilliaryIntegral(x, l, n, exp) / m;

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
			for (j = 0; j < i; j++)
				m *= (k - j);
			f += m * MathUtils.fastPowLoop(x, k - i);
		}

		return f * exp;

	}

	@Override
	public void setIntegrationSegments(NumericProperty integrationSegments) {
		super.setIntegrationSegments(integrationSegments);
		m = this.integrationSegments + 1;
	}

	public NumericProperty getQuadraturePoints() {
		return NumericProperty.derive(NumericPropertyKeyword.QUADRATURE_POINTS, m);
	}

	public void setQuadraturePoints(NumericProperty m) {
		if (m.getType() != NumericPropertyKeyword.QUADRATURE_POINTS)
			throw new IllegalArgumentException("Illegal type: " + m.getType());
		this.m = (int) m.getValue();
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch (type) {
		case INTEGRATION_CUTOFF:
			setCutoff(property);
			break;
		case QUADRATURE_POINTS:
			setQuadraturePoints(property);
			break;
		default:
			return;
		}

		notifyListeners(this, property);

	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<Property>();
		list.add(NumericProperty.def(NumericPropertyKeyword.INTEGRATION_CUTOFF));
		list.add(NumericProperty.def(NumericPropertyKeyword.QUADRATURE_POINTS));
		return list;
	}

	@Override
	public String toString() {
		return getDescriptor() + " : " + getQuadraturePoints() + " ; " + getCutoff();
	}

}