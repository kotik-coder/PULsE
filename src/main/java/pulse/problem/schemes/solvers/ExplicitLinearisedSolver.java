package pulse.problem.schemes.solvers;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ExplicitScheme;
import pulse.problem.statements.ClassicalProblem;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;

/**
 * Performs a fully-dimensionless calculation for the {@code LinearisedProblem}.
 * <p>
 * Relies on using the heat equation to
 * calculate the value of the grid-function at the next timestep. Fills the
 * {@code grid} completely at each specified spatial point. The heating curve is
 * updated with the rear-side temperature
 * <math><i>&Theta;(x<sub>N</sub>,t<sub>i</sub></i></math>) (here
 * <math><i>N</i></math> is the grid density) at the end of {@code timeLimit}
 * intervals, which comprise of {@code timeLimit/tau} time steps. The
 * {@code HeatingCurve} is scaled (re-normalised) by a factor of
 * {@code maxTemp/maxVal}, where {@code maxVal} is the absolute maximum of the
 * calculated solution (with respect to time), and {@code maxTemp} is the
 * {@code maximumTemperature} {@code NumericProperty} of {@code problem}.
 * </p>
 * <p>
 * The explicit scheme uses a standard 4-point template on a one-dimensional
 * grid that utilises the following grid-function values on each step:
 * <math><i>&Theta;(x<sub>i</sub>,t<sub>m</sub>),
 * &Theta;(x<sub>i</sub>,t<sub>m+1</sub>),
 * &Theta;(x<sub>i-1</sub>,t<sub>m</sub>),
 * &Theta;(x<sub>i+1</sub>,t<sub>m</sub>)</i></math>. Hence, the calculation of
 * the grid-function at the timestep <math><i>m</i>+1</math> can be done
 * <i>explicitly</i>. The derivative in the boundary conditions is approximated
 * using a simple forward difference.
 * </p>
 * <p>
 * The explicit scheme is stable only if <math><i>&tau; &le;
 * h<sup>2</sup></i></math> and has an order of approximation of
 * <math><i>O(&tau; + h)</i></math>. Note that this scheme is only used for
 * validating more complex schemes and does not give accurate results due to the
 * lower order of approximation. When calculations using this scheme are
 * performed, the <code>gridDensity</code> is chosen to be at least 80, which
 * ensures that the error is not too high (typically a {@code 1.5E-2} relative
 * error).
 * </p>
 * 
 * @see super.solve(Problem)
 */

public class ExplicitLinearisedSolver extends ExplicitScheme implements Solver<ClassicalProblem> {

	private int N;
	private double hx;
	private double a;

	public ExplicitLinearisedSolver() {
		super();
	}

	public ExplicitLinearisedSolver(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
	}

	public ExplicitLinearisedSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}

	@Override
	public void prepare(Problem problem) {
		super.prepare(problem);

		N = (int) getGrid().getGridDensity().getValue();
		hx = getGrid().getXStep();

		final double Bi1 = (double) problem.getProperties().getHeatLoss().getValue();
		a = 1. / (1. + Bi1 * hx);
	}

	@Override
	public void solve(ClassicalProblem problem) {
		prepare(problem);
		runTimeSequence(problem);
	}
	
	@Override
	public void timeStep(int m) {
		explicitSolution();
		var V = getCurrentSolution();
		setSolutionAt(0, (V[1] + hx * pulse(m)) * a);
		setSolutionAt(N, V[N - 1] * a);
	}

	@Override
	public DifferenceScheme copy() {
		var grid = getGrid();
		return new ExplicitLinearisedSolver(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
	}

	@Override
	public Class<? extends Problem> domain() {
		return ClassicalProblem.class;
	}

}