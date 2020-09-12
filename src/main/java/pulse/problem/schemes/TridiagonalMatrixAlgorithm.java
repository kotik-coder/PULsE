package pulse.problem.schemes;

/**
 * Implements the tridiagonal matrix algorithm (Thomas algorithms) for solving systems of linear equations.
 * Applicable to such systems where the forming matrix has a tridiagonal form.
 *
 */

public class TridiagonalMatrixAlgorithm {

	private Grid grid;
	
	private double a;
	private double b;
	private double c;
	
	private double[] alpha;
	private double[] beta;
	
	public TridiagonalMatrixAlgorithm(Grid grid) {
		this.grid = grid;
		final int N = grid.getGridDensityValue();
		alpha = new double[N + 2];
		beta = new double[N + 2];
	}
	
	/**
	 * Calculates the solution {@code V} using the tridiagonal matrix algorithm.
	 * This performs a backwards sweep from {@code N - 1} to {@code 0} where {@code N}
	 * is the grid density value. The coefficients {@code alpha} and {@code beta} 
	 * should have been precalculated
	 * @param V the array containing the {@code N}th value previously calculated from the respective boundary condition 
	 * @param alpha an array of coefficients for the tridiagonal algorithm
	 * @param beta an array of coefficients for the tridiagonal algorithm
	 */
	
	public void sweep(double[] V) {
		for (int j = grid.getGridDensityValue() - 1; j >= 0; j--) 
			V[j] = alpha[j + 1] * V[j + 1] + beta[j + 1];
	}
	
	/**
	 * Calculates the {@code alpha} coefficients as part of the tridiagonal matrix algorithm.
	 * @param grid the grid 
	 * @param alpha0 the first value &alpha;<sub>1</sub> used to calculate the array
	 * @param a the heat equation coefficient corresponding to &theta;<sub>i+1</sub>
	 * @param b the heat equation coefficient corresponding to &theta;<sub>i</sub>
	 * @param c the heat equation coefficient corresponding to &theta;<sub>i-1</sub>
	 * @return
	 */
	
	public void evaluateAlpha() {
		for (int i = 1, N = grid.getGridDensityValue(); i < N; i++) {
			alpha[i + 1] = c / (b - a * alpha[i]);
		}
	}
	
	public void evaluateBeta(final double[] U) {
		evaluateBeta(U, 2, grid.getGridDensityValue() + 1);
	}

	/**
	 * Calculates the {@code beta} coefficients as part of the tridiagonal matrix algorithm.
	 * @param beta1 the first &beta;<sub>1</sub> coefficient
	 * @param U the input temperature profile
	 * @param a the heat equation coefficient corresponding to &theta;<sub>i+1</sub>
	 * @param b the heat equation coefficient corresponding to &theta;<sub>i</sub>
	 * @return
	 */
	
	public void evaluateBeta(final double[] U, final int start, final int endExclusive) {
		final double tau = grid.getTimeStep();
		for (int i = start; i < endExclusive; i++) 
			beta[i] = beta(U[i - 1] / tau, phi(i - 1), i);
	}
	
	public double beta(final double f, final double phi, final int i) {
		return (f + phi + a * beta[i - 1]) / (b - a * alpha[i - 1]);
	}
	
	public double phi(int i) {
		return 0;
	}
	
	public void setAlpha(final int i, final double alpha) {
		this.alpha[i] = alpha;
	}
	
	public void setBeta(final int i, final double beta) {
		this.beta[i] = beta;
	}

	public double[] getAlpha() {
		return alpha;
	}

	public double[] getBeta() {
		return beta;
	}
	
	public void setCoefA(double a) {
		this.a = a;
	}
	
	public void setCoefB(double b) {
		this.b = b;
	}
	
	public void setCoefC(double c) {
		this.c = c;
	}
	
	protected double getCoefA() {
		return a;
	}
	
	protected double getCoefB() {
		return b;
	}
	
	protected double getCoefC() {
		return c;
	}

	public Grid getGrid() {
		return grid;
	}
	
}