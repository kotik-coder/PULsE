package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.MathUtils;

public class GaussianQuadrature {

	private LegendrePoly poly;

	private double[] roots;

	private double[] nodes;
	private double[] weights;

	private int n;

	public GaussianQuadrature(final int n) {
		poly = new LegendrePoly(n / 2);
		roots = poly.roots();
		this.n = n;
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

	public static void main(String[] args) {
		var q = new GaussianQuadrature(4);
		q.init();
		for (int i = 0; i < q.n; i++)
			System.out.printf("%n{%3.15f \t %3.15f}", q.nodes[i], q.weights[i]);
	}

	public void init() {
		nodes();
		weights();
	}

}