package pulse.problem.schemes.solvers;

import static java.lang.Math.pow;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.statements.PenetrationProblem;
import pulse.problem.statements.Problem;
import pulse.problem.statements.penetration.AbsorptionModel.SpectralRange;
import pulse.properties.NumericProperty;

public class ImplicitTranslucentSolver extends ImplicitScheme implements Solver<PenetrationProblem> {

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

	public ImplicitTranslucentSolver() {
		super();
	}

	public ImplicitTranslucentSolver(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
	}

	public ImplicitTranslucentSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}

	private void prepare(PenetrationProblem problem) {
		super.prepare(problem);

		var grid = getGrid();

		N = (int) grid.getGridDensity().getValue();
		hx = grid.getXStep();
		tau = grid.getTimeStep();

		U = new double[N + 1];
		V = new double[N + 1];
		alpha = new double[N + 2];
		beta = new double[N + 2];

		// coefficients for difference equation

		a = 1. / pow(hx, 2);
		b = 1. / tau + 2. / pow(hx, 2);
		c = 1. / pow(hx, 2);

	}

	@Override
	public void solve(PenetrationProblem problem) {
		prepare(problem);
		var curve = problem.getHeatingCurve();
		var absorption = problem.getAbsorptionModel();

		// precalculated constants

		double HH = pow(hx, 2);
		double maxVal = 0;

		// precalculated constants

		final double Bi1H = (double) problem.getHeatLoss().getValue() * hx;

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

				alpha[1] = 1.0 / (1.0 + HH / (2.0 * tau) + Bi1H);
				beta[1] = (U[0] + tau * pls * absorption.absorption(SpectralRange.LASER, 0.0))
						/ (1.0 + 2.0 * tau / HH * (1 + Bi1H));

				for (int i = 1; i < N; i++) {
					alpha[i + 1] = c / (b - a * alpha[i]);
					double F = -U[i] / tau - pls * absorption.absorption(SpectralRange.LASER, (i - EPS) * hx);
					beta[i + 1] = (F - a * beta[i]) / (a * alpha[i] - b);
				}

				V[N] = (HH * (U[N] + tau * pls * absorption.absorption(SpectralRange.LASER, (N - EPS) * hx))
						+ 2. * tau * beta[N]) / (2 * Bi1H * tau + HH + 2. * tau * (1 - alpha[N]));

				for (int j = N - 1; j >= 0; j--) {
					V[j] = alpha[j + 1] * V[j + 1] + beta[j + 1];
				}

				System.arraycopy(V, 0, U, 0, N + 1);

			}

			double signal = 0;

			for (int i = 0; i < N; i++) {
				signal += V[N - i] * absorption.absorption(SpectralRange.THERMAL, i * hx)
						+ V[N - 1 - i] * absorption.absorption(SpectralRange.THERMAL, (i + 1) * hx);
			}

			signal *= hx / 2.0;

			maxVal = Math.max(maxVal, signal);

			curve.addPoint((w * getTimeInterval()) * tau * problem.timeFactor(), signal);

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
		return new ImplicitTranslucentSolver(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
	}

	@Override
	public Class<? extends Problem> domain() {
		return PenetrationProblem.class;
	}

}