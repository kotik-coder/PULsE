package pulse.problem.schemes.solvers;

import static java.lang.Math.pow;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.MixedScheme;
import pulse.problem.statements.LinearisedProblem;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;

/**
 * Performs a fully-dimensionless calculation for the {@code LinearisedProblem}.
 * <p>
 * Calls {@code super.solve(Problem)}, then initiates constants for calculations
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
 * The semi-implicit scheme uses a 6-point template on a one-dimensional grid
 * that utilises the following grid-function values on each step:
 * <math><i>&Theta;(x<sub>i</sub>,t<sub>m</sub>),
 * &Theta;(x<sub>i</sub>,t<sub>m+1</sub>),
 * &Theta;(x<sub>i-1</sub>,t<sub>m</sub>),
 * &Theta;(x<sub>i+1</sub>,t<sub>m</sub>),
 * &Theta;(x<sub>i-1</sub>,t<sub>m+1</sub>),
 * &Theta;(x<sub>i+1</sub>,t<sub>m+1</sub>)</i></math>. The boundary conditions
 * are approximated with a Taylor expansion up to the third term, hence the
 * scheme has an increased order of approximation.
 * </p>
 * <p>
 * The semi-implicit scheme is unconditionally stable and has an order of
 * approximation of <math><i>O(&tau;<sup>2</sup> + h<sup>2</sup>)</i></math>.
 * Note this scheme is prone to spurious oscillations when either a high spatial
 * resolution or a large timestep are used. It has been noticed that due to the
 * pulse term in the boundary condition, a higher error is introduced into the
 * calculation than for the implicit scheme.
 * </p>
 * 
 * @see super.solve(Problem)
 */

public class MixedLinearisedSolver extends MixedScheme implements Solver<LinearisedProblem> {

	private int N;
	private double hx;
	private double tau;

	private double a;
	private double b;
	private double c;
	private double a1;
	private double b1;
	private double c1;
	private double b2;
	private double b3;
	private double c2;

	private double[] U;
	private double[] V;
	private double[] alpha;
	private double[] beta;

	private final static double EPS = 1e-7; // a small value ensuring numeric stability

	public MixedLinearisedSolver() {
		super();
	}

	public MixedLinearisedSolver(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
	}

	public MixedLinearisedSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
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
		alpha = new double[N + 2];
		beta = new double[N + 2];

		// coefficients for the finite-difference heat equation

		a = 1. / pow(hx, 2);
		b = 2. / tau + 2. / pow(hx, 2);
		c = 1. / pow(hx, 2);

		// precalculated constants

		final double HH = pow(hx, 2);

		final double Bi1HTAU = Bi1 * hx * tau;

		// constant for boundary-conditions calculation

		a1 = tau / (Bi1HTAU + HH + tau);
		b1 = 1. / (Bi1HTAU + HH + tau);
		b2 = -hx * (Bi1 * tau - hx);
		b3 = hx * tau;
		c1 = b2;
		c2 = Bi1HTAU + HH;
	}

	@Override
	public void solve(LinearisedProblem problem) {
		this.prepare(problem);
		var curve = problem.getHeatingCurve();

		// precalculated constants

		final double HH = pow(hx, 2);
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

				alpha[1] = a1;
				double pls = discretePulse.laserPowerAt((m - 1 + EPS) * tau) + discretePulse.laserPowerAt((m - EPS) * tau);
				beta[1] = b1 * (b2 * U[0] + b3 * pls - tau * (U[0] - U[1]));

				for (int i = 1; i < N; i++) {
					alpha[i + 1] = c / (b - a * alpha[i]);
					double F = -2. * U[i] / tau - (U[i + 1] - 2. * U[i] + U[i - 1]) / HH;
					beta[i + 1] = (F - a * beta[i]) / (a * alpha[i] - b);
				}

				V[N] = (c1 * U[N] + tau * beta[N] - tau * (U[N] - U[N - 1])) / (c2 - tau * (alpha[N] - 1));

				for (int j = N - 1; j >= 0; j--) {
					V[j] = alpha[j + 1] * V[j + 1] + beta[j + 1];
				}

				System.arraycopy(V, 0, U, 0, N + 1);

			}

			maxVal = Math.max(maxVal, V[N]);

			curve.addPoint((w * getTimeInterval()) * tau * problem.timeFactor(), V[N]);

		}

		final double maxTemp = (double) problem.getMaximumTemperature().getValue();
		curve.scale(maxTemp / maxVal);

	}

	@Override
	public DifferenceScheme copy() {
		var grid = getGrid();
		return new MixedLinearisedSolver(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
	}

	@Override
	public Class<? extends Problem> domain() {
		return LinearisedProblem.class;
	}

}