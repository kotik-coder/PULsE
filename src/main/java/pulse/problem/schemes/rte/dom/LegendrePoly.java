package pulse.problem.schemes.rte.dom;

import org.apache.commons.math3.analysis.solvers.LaguerreSolver;

import pulse.problem.schemes.rte.MathUtils;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;

public class LegendrePoly {

	/**
	 * Fast calculation of binomial coefficient C_m^k = m!/(k!(m-k)!)
	 * 
	 * @param m integer.
	 * @param k integer, k <= m.
	 * @return binomial coefficient C_m^k.
	 */

	public static long binomial(int m, int k) {
		int k1 = m - k;
		k = k > k1 ? k1 : k;

		long c = 1;

		// Calculate value of Binomial Coefficient in bottom up manner
		for (int i = 1; i < k + 1; i++, m--)
			c = c / i * m + c % i * m / i; // split c * n / i into (c / i * i + c % i) * n / i

		return c;

	}

	/**
	 * Fast calculation of binomial coefficient C_m^k = m!/(k!(m-k)!)
	 * 
	 * @param m integer.
	 * @param k integer, k <= m.
	 * @return binomial coefficient C_m^k.
	 */

	public static double generalisedBinomial(double alpha, int k) {

		double c = 1;

		for (int i = 0; i < k; i++)
			c *= (alpha - i) / (k - i);

		return c;

	}

	protected double[] c;
	protected int n;

	protected LaguerreSolver solver;

	protected double solverError;

	public LegendrePoly(final int n) {
		this.n = n;
		c = new double[n + 1];
		this.solverError = (double) NumericProperty.theDefault(NumericPropertyKeyword.LAGUERRE_SOLVER_ERROR).getValue();
		solver = new LaguerreSolver(solverError);
		coefficients();
	}

	/**
	 * This will generate the coefficients for the Legendre polynomial, arranged in
	 * order of significance (from x^0 to x^n).
	 */

	public void coefficients() {

		long intFactor = MathUtils.fastPowInt(2, n);

		for (int i = 0; i < c.length; i++)
			c[i] = intFactor * binomial(n, i) * generalisedBinomial((n + i - 1) * 0.5, n);

	}

	public double derivative(double x) {
		double d = 0;

		for (int i = 1; i < c.length; i++)
			d += i * c[i] * MathUtils.fastPowLoop(x, i - 1);

		return d;

	}

	public int getOrder() {
		return n;
	}

	public double poly(double x) {
		double poly = 0;

		for (int i = 0; i < c.length; i++)
			poly += c[i] * MathUtils.fastPowLoop(x, i);

		return poly;

	}
	
	public double[] roots() {
		var complexRoots = solver.solveAllComplex(c, 1.0);
		var roots = new double[n];
		
		// the last roots is always zero, so we have n non-zero roots in total
		// in case of even n, the first n/2 roots are positive and the rest are negative
		for (int i = 0; i < n; i++) {
			roots[i] = complexRoots[i].getReal();
			System.out.println(roots[i]);
		}
		
		return roots;

	}

}