package pulse.problem.schemes.solvers;

import static java.lang.Math.pow;

import pulse.HeatingCurve;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ImplicitScheme;
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
 * @see super.solve(Problem)
 */

public class ImplicitLinearisedSolver 
					extends ImplicitScheme 
							implements Solver<LinearisedProblem> {

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
	public void solve(LinearisedProblem problem) {

		super.prepare(problem);
		
		int N		= (int)grid.getGridDensity().getValue();
		double hx	= grid.getXStep();
		double tau	= grid.getTimeStep();

		final double Bi1 = (double) problem.getFrontHeatLoss().getValue();
		final double Bi2 = (double) problem.getHeatLossRear().getValue();
		final double maxTemp = (double) problem.getMaximumTemperature().getValue();

		final double EPS = 1e-7; // a small value ensuring numeric stability

		double[] U = new double[N + 1];
		double[] V = new double[N + 1];
		double[] alpha = new double[N + 1];
		double[] beta = new double[N + 1];

		HeatingCurve curve = problem.getHeatingCurve();
		curve.reinit();
		final int counts = (int) curve.getNumPoints().getValue();

		double maxVal = 0;
		int i, j, m, w;
		double pls;

		// coefficients for difference equation

		double a = 1. / pow(hx, 2);
		double b = 1. / tau + 2. / pow(hx, 2);
		double c = 1. / pow(hx, 2);

		// precalculated constants

		double HH = pow(hx, 2);
		double _2HTAU = 2. * hx * tau;

		double F;

		// precalculated constants

		double Bi1HTAU = Bi1 * hx * tau;
		double Bi2HTAU = Bi2 * hx * tau;

		/*
		 * The outer cycle iterates over the number of points of the HeatingCurve
		 */

		for (w = 1; w < counts; w++) {

			/*
			 * Two adjacent points of the heating curves are separated by timeInterval on
			 * the time grid. Thus, to calculate the next point on the heating curve,
			 * timeInterval/tau time steps have to be made first.
			 */

			for (m = (w - 1) * timeInterval + 1; m < w * timeInterval + 1; m++) {

				pls = discretePulse.evaluateAt((m - EPS) * tau); // NOTE: EPS is very important here and ensures
																		// numeric stability!

				alpha[1] = 2. * tau / (2. * Bi1HTAU + 2. * tau + HH);
				beta[1] = (HH * U[0] + _2HTAU * pls) / (2. * Bi1HTAU + 2. * tau + HH);

				for (i = 1; i < N; i++) {
					alpha[i + 1] = c / (b - a * alpha[i]);
					F = -U[i] / tau;
					beta[i + 1] = (F - a * beta[i]) / (a * alpha[i] - b);
				}

				V[N] = (HH * U[N] + 2. * tau * beta[N])
						/ (2 * Bi2HTAU + HH - 2. * tau * (alpha[N] - 1));

				for (j = N - 1; j >= 0; j--) 
					V[j] = alpha[j + 1] * V[j + 1] + beta[j + 1];

				System.arraycopy(V, 0, U, 0, N + 1);

			}

			maxVal = Math.max(maxVal, V[N]);
			curve.addPoint(
					(w * timeInterval) * tau * problem.timeFactor(),
					V[N] );

			/*
			 * UNCOMMENT TO DEBUG
			 */

			//debug(problem, V, w);

		}

		curve.scale(maxTemp / maxVal);

	}
	
	@Override
	public DifferenceScheme copy() {
		return new ImplicitLinearisedSolver(grid.getGridDensity(),
				grid.getTimeFactor(), getTimeLimit());
	}
	
	@Override
	public Class<? extends Problem> domain() {
		return LinearisedProblem.class;
	}

}