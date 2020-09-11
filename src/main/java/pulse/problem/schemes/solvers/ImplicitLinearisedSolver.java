package pulse.problem.schemes.solvers;

import static java.lang.Math.pow;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.statements.LinearisedProblem;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;

/**
 * Performs a fully-dimensionless calculation for the {@code LinearisedProblem}.
 * <p>
 * Initiates constants for calculations
 * and uses a sweep method to evaluate the solution for each subsequent
 * timestep, filling the {@code grid} completely at each specified spatial
 * point. The heating curve is updated with the rear-side temperature
 * <math><i>&Theta;(x<sub>N</sub>,t<sub>i</sub></i></math>) (here
 * <math><i>N</i></math> is the grid density) at the end of {@code timeLimit}
 * intervals, which comprise of {@code timeLimit/tau} time steps. The
 * {@code HeatingCurve} is scaled (re-normalised) by a factor of
 * {@code maxTemp/maxVal}, where {@code maxVal} is the absolute maximum of the
 * calculated solution (with respect to time), and {@code maxTemp} is the
 * {@code maximumTemperature} {@code NumericProperty} of {@code problem}.
 * </p>
 * 
 * <p>
 * The fully implicit scheme uses a standard 4-point template on a
 * one-dimensional grid that utilises the following grid-function values on each
 * step: <math><i>&Theta;(x<sub>i</sub>,t<sub>m</sub>),
 * &Theta;(x<sub>i</sub>,t<sub>m+1</sub>),
 * &Theta;(x<sub>i-1</sub>,t<sub>m+1</sub>),
 * &Theta;(x<sub>i+1</sub>,t<sub>m+1</sub>)</i></math>. Because no
 * <i>explicit</i> formula can be used for calculating the grid-function at
 * timestep <math><i>m</i>+1</math>, a sweep method is implemented instead. The
 * boundary conditions are approximated with a Taylor expansion up to the third
 * term, hence the scheme has an increased order of approximation.
 * </p>
 * <p>
 * The fully implicit scheme is unconditionally stable and has an order of
 * approximation of at least <math><i>O(&tau; + h<sup>2</sup>)</i></math> for
 * both the heat equation and the boundary conditions.
 * </p>
 * 
 * @see super.solve(Problem)
 */

public class ImplicitLinearisedSolver extends ImplicitScheme implements Solver<LinearisedProblem> {

	private double Bi1HTAU;

	private int N;
	private double tau;
	
	private double HH;
	private double _2HTAU;

	private final static double EPS = 1e-7; // a small value ensuring numeric stability

	public ImplicitLinearisedSolver() {
		super();
	}

	public ImplicitLinearisedSolver(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
	}

	public ImplicitLinearisedSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}

	@Override
	public void prepare(Problem problem) {
		super.prepare(problem);

		var grid = getGrid();

		N = (int) grid.getGridDensity().getValue();
		final double hx = grid.getXStep();
		tau = grid.getTimeStep();

		final double Bi1 = (double) problem.getHeatLoss().getValue();

		Bi1HTAU = Bi1 * hx * tau;
		
		// precalculated constants

		HH = hx*hx;
		_2HTAU = 2. * hx * tau;
				
		final double alpha0 = 2. * tau / (2. * Bi1HTAU + 2. * tau + hx*hx);
		final var tridiagonal = getTridiagonalMatrixAlgorithm();
		tridiagonal.setAlpha(1, alpha0);
		
		// coefficients for difference equation

		tridiagonal.setCoefA( 1. / pow(hx, 2) );
		tridiagonal.setCoefB( 1. / tau + 2. / pow(hx, 2) );
		tridiagonal.setCoefC( 1. / pow(hx, 2) );		
		
		tridiagonal.evaluateAlpha();
	}

	@Override
	public void solve(LinearisedProblem problem) {
		prepare(problem);
		runTimeSequence(problem);
	}
	
	@Override
	public double firstBeta(final int m) {
		final double pls = getDiscretePulse().laserPowerAt((m - EPS) * tau); // NOTE: EPS is very important here and ensures numeric stability!
		return (HH * getPreviousSolution()[0] + _2HTAU * pls) / (2. * Bi1HTAU + 2. * tau + HH);
	}
	
	@Override
	public double evalRightBoundary(final int m, final double alphaN, final double betaN) {
		return (HH * getPreviousSolution()[N] + 2. * tau * betaN) / (2 * Bi1HTAU + HH - 2. * tau * (alphaN - 1));
	}

	@Override
	public DifferenceScheme copy() {
		var grid = getGrid();
		return new ImplicitLinearisedSolver(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
	}

	@Override
	public Class<? extends Problem> domain() {
		return LinearisedProblem.class;
	}

}