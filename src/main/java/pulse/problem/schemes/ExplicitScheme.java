package pulse.problem.schemes;

import static pulse.math.MathUtils.fastPowLoop;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericPropertyKeyword.GRID_DENSITY;
import static pulse.properties.NumericPropertyKeyword.TAU_FACTOR;
import static pulse.ui.Messages.getString;

import pulse.properties.NumericProperty;

/**
 * This class provides the necessary framework to enable a simple explicit
 * finite-difference scheme (also called the forward-time centred space scheme)
 * for solving the one-dimensional heat conduction problem.
 * 
 * @see pulse.problem.statements.LinearisedProblem
 * @see pulse.problem.statements.NonlinearProblem
 *
 */

public abstract class ExplicitScheme extends OneDimensionalScheme {

	/**
	 * Constructs a default explicit scheme using the default values of
	 * {@code GRID_DENSITY} and {@code TAU_FACTOR}.
	 */

	public ExplicitScheme() {
		this(derive(GRID_DENSITY, 80), derive(TAU_FACTOR, 0.5));
	}

	/**
	 * Constructs an explicit scheme on a one-dimensional grid that is specified by
	 * the values {@code N} and {@code timeFactor}.
	 * 
	 * @see pulse.problem.schemes.DifferenceScheme
	 * @param N          the {@code NumericProperty} with the type
	 *                   {@code GRID_DENSITY}
	 * @param timeFactor the {@code NumericProperty} with the type
	 *                   {@code TAU_FACTOR}
	 */

	public ExplicitScheme(NumericProperty N, NumericProperty timeFactor) {
		super();
		setGrid(new Grid(N, timeFactor));
	}

	/**
	 * <p>
	 * Constructs an explicit scheme on a one-dimensional grid that is specified by
	 * the values {@code N} and {@code timeFactor}. Sets the time limit of this
	 * scheme to {@code timeLimit}
	 * 
	 * @param N          the {@code NumericProperty} with the type
	 *                   {@code GRID_DENSITY}
	 * @param timeFactor the {@code NumericProperty} with the type
	 *                   {@code TAU_FACTOR}
	 * @param timeLimit  the {@code NumericProperty} with the type
	 *                   {@code TIME_LIMIT}
	 * @see pulse.problem.schemes.DifferenceScheme
	 */

	public ExplicitScheme(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(timeLimit);
		setGrid(new Grid(N, timeFactor));
	}
	
	/**
	 * Uses the heat equation explicitly to calculate the grid-function everywhere
	 * except for the boundaries
	 * @param grid the grid 
	 * @param V the output temperature profile 
	 * @param U the input temperature profile
	 */
	
	public void explicitSolution() {
		var grid = getGrid();
		var U = getPreviousSolution();
		final double TAU_HH = grid.getTimeStep()/(fastPowLoop(grid.getXStep(), 2));
		for (int i = 1, N = grid.getGridDensityValue(); i < N; i++) 
			setSolutionAt(i, U[i] + TAU_HH * (U[i + 1] - 2. * U[i] + U[i - 1]) + phi(i) );
	}
	
	public double phi(final int i) {
		return 0;
	}

	/**
	 * Prints out the description of this problem type.
	 * 
	 * @return a verbose description of the problem.
	 */

	@Override
	public String toString() {
		return getString("ExplicitScheme.4");
	}

}