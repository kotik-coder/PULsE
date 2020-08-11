package pulse.problem.schemes.solvers;

import static java.lang.Math.pow;
import static pulse.ui.Messages.getString;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.DistributedDetection;
import pulse.problem.schemes.ExplicitScheme;
import pulse.problem.statements.PenetrationProblem;
import pulse.problem.statements.Problem;
import pulse.problem.statements.penetration.AbsorptionModel.SpectralRange;
import pulse.properties.NumericProperty;

public class ExplicitTranslucentSolver extends ExplicitScheme implements Solver<PenetrationProblem> {

	private int N;
	private double hx;
	private double tau;

	private double a;

	private double[] U;
	private double[] V;
	private final static double EPS = 1e-7; // a small value ensuring numeric stability

	public ExplicitTranslucentSolver() {
		super();
	}

	public ExplicitTranslucentSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
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

		double Bi1 = (double) problem.getHeatLoss().getValue();

		a = 1. / (1. + Bi1 * hx);

	}

	@Override
	public void solve(PenetrationProblem problem) {
		prepare(problem);
		
		var absorb = problem.getAbsorptionModel();
		var curve = problem.getHeatingCurve();
		var grid = getGrid();
		
		final double TAU_HH = tau / pow(hx, 2);

		double maxVal = 0;
		final double maxTemp = (double) problem.getMaximumTemperature().getValue();
		
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

				double pls = discretePulse.laserPowerAt((m - EPS) * tau);

				/*
				 * Uses the heat equation explicitly to calculate the grid-function everywhere
				 * except the boundaries
				 */
				for (int i = 1; i < N; i++) {
					V[i] = U[i] + TAU_HH * (U[i + 1] - 2. * U[i] + U[i - 1])
							+ tau * pls * absorb.absorption(SpectralRange.LASER, (i - EPS) * hx);
				}

				/*
				 * Calculates boundary values
				 */

				V[0] = V[1] * a;
				V[N] = V[N - 1] * a;

				System.arraycopy(V, 0, U, 0, N + 1);

			}

			double signal = DistributedDetection.evaluateSignal(absorb, grid, V);

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

	/**
	 * Prints out the description of this problem type.
	 * 
	 * @return a verbose description of the problem.
	 */

	@Override
	public String toString() {
		return getString("ExplicitScheme.4");
	}

	@Override
	public Class<? extends Problem> domain() {
		return PenetrationProblem.class;
	}

}