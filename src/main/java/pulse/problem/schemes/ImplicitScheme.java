package pulse.problem.schemes;

import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericPropertyKeyword.GRID_DENSITY;
import static pulse.properties.NumericPropertyKeyword.TAU_FACTOR;
import static pulse.ui.Messages.getString;

import pulse.properties.NumericProperty;

/**
 * An abstract implicit finite-difference scheme for solving one-dimensional
 * heat conduction problems.
 * 
 * @see pulse.problem.statements.LinearisedProblem
 * @see pulse.problem.statements.NonlinearProblem
 */

public abstract class ImplicitScheme extends DifferenceScheme {

	/**
	 * Constructs a default fully-implicit scheme using the default values of
	 * {@code GRID_DENSITY} and {@code TAU_FACTOR}.
	 */

	public ImplicitScheme() {
		this(derive(GRID_DENSITY, 30), derive(TAU_FACTOR, 0.25));
	}

	/**
	 * Constructs a fully-implicit scheme on a one-dimensional grid that is
	 * specified by the values {@code N} and {@code timeFactor}.
	 * 
	 * @see pulse.problem.schemes.DifferenceScheme
	 * @param N          the {@code NumericProperty} with the type
	 *                   {@code GRID_DENSITY}
	 * @param timeFactor the {@code NumericProperty} with the type
	 *                   {@code TAU_FACTOR}
	 */

	public ImplicitScheme(NumericProperty N, NumericProperty timeFactor) {
		super();
		setGrid(new Grid(N, timeFactor));
	}

	/**
	 * <p>
	 * Constructs a fully-implicit scheme on a one-dimensional grid that is
	 * specified by the values {@code N} and {@code timeFactor}. Sets the time limit
	 * of this scheme to {@code timeLimit}
	 * 
	 * @param N          the {@code NumericProperty} with the type
	 *                   {@code GRID_DENSITY}
	 * @param timeFactor the {@code NumericProperty} with the type
	 *                   {@code TAU_FACTOR}
	 * @param timeLimit  the {@code NumericProperty} with the type
	 *                   {@code TIME_LIMIT}
	 * @see pulse.problem.schemes.DifferenceScheme
	 */

	public ImplicitScheme(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(timeLimit);
		setGrid(new Grid(N, timeFactor));
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
	
	public static void sweep(Grid grid, double[] V, final double[] alpha, final double[] beta) {
		for (int j = grid.getGridDensityValue() - 1; j >= 0; j--) 
			V[j] = alpha[j + 1] * V[j + 1] + beta[j + 1];
	}
	
	/**
	 * Calculates the {@code alpha} coefficients as part of the tridiagonal matrix algorithm.
	 * @param grid the grid 
	 * @param alpha0 the first value &alpha;<sub>0</sub> used to calculate the array
	 * @param a the heat equation coefficient corresponding to &theta;<sub>i+1</sub>
	 * @param b the heat equation coefficient corresponding to &theta;<sub>i</sub>
	 * @param c the heat equation coefficient corresponding to &theta;<sub>i-1</sub>
	 * @return
	 */
	
	public static double[] alpha(Grid grid, final double alpha0, final double a, final double b, final double c) {
		final int N = grid.getGridDensityValue();
		var alpha = new double[N + 2];
		alpha[1] = alpha0;
		for (int i = 1; i < N; i++) {
			alpha[i + 1] = c / (b - a * alpha[i]);
		}
		return alpha;
	}

	/**
	 * Prints out the description of this problem type.
	 * 
	 * @return a verbose description of the problem.
	 */

	@Override
	public String toString() {
		return getString("ImplicitScheme.4");
	}

}