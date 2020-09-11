package pulse.problem.schemes;

import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericPropertyKeyword.GRID_DENSITY;
import static pulse.properties.NumericPropertyKeyword.TAU_FACTOR;
import static pulse.ui.Messages.getString;

import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;

/**
 * An abstract implicit finite-difference scheme for solving one-dimensional
 * heat conduction problems.
 * 
 * @see pulse.problem.statements.LinearisedProblem
 * @see pulse.problem.statements.NonlinearProblem
 */

public abstract class ImplicitScheme extends OneDimensionalScheme {
	
	private TridiagonalMatrixAlgorithm tridiagonal;

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
	
	@Override
	protected void prepare(Problem problem) {
		super.prepare(problem);
		tridiagonal = new TridiagonalMatrixAlgorithm(getGrid());
	}
	
	@Override
	public void timeStep(final int m) {
		leftBoundary(m);
		final var V = getCurrentSolution();
		final int N = V.length - 1;
		setSolutionAt(N, evalRightBoundary(m, tridiagonal.getAlpha()[N], tridiagonal.getBeta()[N]) );
		tridiagonal.sweep(V);
	}
	
	public void leftBoundary(final int m) {
		tridiagonal.setBeta( 1, firstBeta(m) );
		tridiagonal.evaluateBeta(getPreviousSolution());
	}	
	
	public abstract double evalRightBoundary(final int m, final double alphaN, final double betaN); 
	public abstract double firstBeta(final int m);

	/**
	 * Prints out the description of this problem type.
	 * 
	 * @return a verbose description of the problem.
	 */

	@Override
	public String toString() {
		return getString("ImplicitScheme.4");
	}

	public TridiagonalMatrixAlgorithm getTridiagonalMatrixAlgorithm() {
		return tridiagonal;
	}

	public void setTridiagonalMatrixAlgorithm(TridiagonalMatrixAlgorithm tridiagonal) {
		this.tridiagonal = tridiagonal;
	}

}