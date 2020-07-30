package pulse.problem.schemes.solvers;

import static java.lang.Math.pow;

import pulse.HeatingCurve;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ExplicitScheme;
import pulse.problem.statements.PenetrationProblem;
import pulse.problem.statements.Problem;
import pulse.problem.statements.penetration.AbsorptionModel;
import pulse.problem.statements.penetration.AbsorptionModel.SpectralRange;
import pulse.properties.NumericProperty;

public class ExplicitTranslucentSolver extends ExplicitScheme implements Solver<PenetrationProblem> {

	private double maxTemp;
	private AbsorptionModel absorb;

	private int N;
	private int counts;
	private double hx;
	private double tau;

	private HeatingCurve curve;

	private double a, b;

	private double[] U;
	private double[] V;
	private double maxVal;

	private final static double EPS = 1e-7; // a small value ensuring numeric stability

	public ExplicitTranslucentSolver() {
		super();
	}

	public ExplicitTranslucentSolver(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
	}

	public ExplicitTranslucentSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}

	private void prepare(PenetrationProblem problem) {
		super.prepare(problem);
		curve = problem.getHeatingCurve();

		absorb = problem.getAbsorptionModel();

		var grid = getGrid();

		N = (int) grid.getGridDensity().getValue();
		hx = grid.getXStep();
		tau = grid.getTimeStep();

		U = new double[N + 1];
		V = new double[N + 1];

		double Bi1 = (double) problem.getHeatLoss().getValue();
		double Bi2 = Bi1;
		maxTemp = (double) problem.getMaximumTemperature().getValue();

		counts = (int) curve.getNumPoints().getValue();

		maxVal = 0;

		a = 1. / (1. + Bi1 * hx);
		b = 1. / (1. + Bi2 * hx);

	}

	@Override
	public void solve(PenetrationProblem problem) {
		prepare(problem);

		double TAU_HH = tau / pow(hx, 2);

		double pls;
		double signal = 0;

		int i, m, w;

		final var discretePulse = getDiscretePulse();

		/*
		 * The outer cycle iterates over the number of points of the HeatingCurve
		 */

		for (w = 1; w < counts; w++) {

			/*
			 * Two adjacent points of the heating curves are separated by timeInterval on
			 * the time grid. Thus, to calculate the next point on the heating curve,
			 * timeInterval/tau time steps have to be made first.
			 */

			for (m = (w - 1) * getTimeInterval() + 1; m < w * getTimeInterval() + 1; m++) {

				pls = discretePulse.laserPowerAt((m - EPS) * tau);

				/*
				 * Uses the heat equation explicitly to calculate the grid-function everywhere
				 * except the boundaries
				 */
				for (i = 1; i < N; i++) {
					V[i] = U[i] + TAU_HH * (U[i + 1] - 2. * U[i] + U[i - 1])
							+ tau * pls * absorb.absorption(SpectralRange.LASER, (i - EPS) * hx);
				}

				/*
				 * Calculates boundary values
				 */

				V[0] = V[1] * a;
				V[N] = V[N - 1] * b;

				System.arraycopy(V, 0, U, 0, N + 1);

			}

			signal = 0;

			for (i = 0; i < N; i++) {
				signal += V[N - i] * absorb.absorption(SpectralRange.THERMAL, i * hx)
						+ V[N - 1 - i] * absorb.absorption(SpectralRange.THERMAL, (i + 1) * hx);
			}

			signal *= hx / 2.0;

			maxVal = Math.max(maxVal, signal);

			curve.addPoint((w * getTimeInterval()) * tau * problem.timeFactor(), signal);

		}

		curve.scale(maxTemp / maxVal);

	}

	@Override
	public DifferenceScheme copy() {
		var grid = getGrid();
		return new ExplicitTranslucentSolver(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
	}

	@Override
	public Class<? extends Problem> domain() {
		return PenetrationProblem.class;
	}

}