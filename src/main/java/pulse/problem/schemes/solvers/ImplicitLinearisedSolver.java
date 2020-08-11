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
	private double hx;
	private double tau;

	private double a;
	private double b;
	private double c;

	private double[] U;
	private double[] V;
	private double[] alpha;
	private double[] beta;

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
		hx = grid.getXStep();
		tau = grid.getTimeStep();

		final double Bi1 = (double) problem.getHeatLoss().getValue();

		U = new double[N + 1];
		V = new double[N + 1];
		alpha = new double[N + 1];
		beta = new double[N + 1];

		// coefficients for difference equation

		a = 1. / pow(hx, 2);
		b = 1. / tau + 2. / pow(hx, 2);
		c = 1. / pow(hx, 2);

		Bi1HTAU = Bi1 * hx * tau;
	}

	@Override
	public void solve(LinearisedProblem problem) {
		prepare(problem);
		var curve = problem.getHeatingCurve();

		// precalculated constants

		double HH = pow(hx, 2);
		double _2HTAU = 2. * hx * tau;

		double maxVal = 0;

		final var discretePulse = getDiscretePulse();
		
		/*
		 * The outer cycle iterates over the number of points of the HeatingCurve
		 */

		for (int w = 1, counts = (int) curve.getNumPoints().getValue(); w < counts; w++) {

			/*
			 * Two adjacent points of the heating curves are separated by timeInterval on
			 * the time grid. Thus, to calculate the next point on the heating curve,
			 * timeInterval/tau time steps have to be made first.
			 */

			for (int m = (w - 1) * getTimeInterval() + 1; m < w * getTimeInterval() + 1; m++) {

				double pls = discretePulse.laserPowerAt((m - EPS) * tau); // NOTE: EPS is very important here and ensures
																	// numeric stability!

				alpha[1] = 2. * tau / (2. * Bi1HTAU + 2. * tau + HH);
				beta[1] = (HH * U[0] + _2HTAU * pls) / (2. * Bi1HTAU + 2. * tau + HH);

				for (int i = 1; i < N; i++) {
					alpha[i + 1] = c / (b - a * alpha[i]);
					double F = -U[i] / tau;
					beta[i + 1] = (F - a * beta[i]) / (a * alpha[i] - b);
				}

				V[N] = (HH * U[N] + 2. * tau * beta[N]) / (2 * Bi1HTAU + HH - 2. * tau * (alpha[N] - 1));

				for (int j = N - 1; j >= 0; j--) {
					V[j] = alpha[j + 1] * V[j + 1] + beta[j + 1];
				}

				System.arraycopy(V, 0, U, 0, N + 1);

			}

			maxVal = Math.max(maxVal, V[N]);
			curve.addPoint((w * getTimeInterval()) * tau * problem.timeFactor(), V[N]);

			/*
			 * UNCOMMENT TO DEBUG
			 */

			// debug(problem, V, w);

		}

		final double maxTemp = (double) problem.getMaximumTemperature().getValue();
		curve.scale(maxTemp / maxVal);

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