package pulse.problem.schemes.solvers;

import static java.lang.Math.pow;

import pulse.HeatingCurve;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.statements.DiathermicMaterialProblem;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;

public class ImplicitDiathermicSolver extends ImplicitScheme implements Solver<DiathermicMaterialProblem> {

	private final static double EPS = 1e-7; // a small value ensuring numeric stability

	private double[] U, V;
	private double[] p, q;
	private double a, b, c;
	private double[] alpha, beta, gamma;

	private double Bi1;
	private double maxTemp;
	private double eta;

	private double maxVal;

	private int N;
	private int counts;
	private double hx;
	private double tau;

	private HeatingCurve curve;

	public ImplicitDiathermicSolver() {
		super();
	}

	public ImplicitDiathermicSolver(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
	}

	public ImplicitDiathermicSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}

	private void prepare(DiathermicMaterialProblem problem) {
		super.prepare(problem);
		curve = problem.getHeatingCurve();

		Bi1 = (double) problem.getFrontHeatLoss().getValue();
		maxTemp = (double) problem.getMaximumTemperature().getValue();
		eta = (double) problem.getDiathermicCoefficient().getValue();

		N = (int) grid.getGridDensity().getValue();
		hx = grid.getXStep();
		tau = grid.getTimeStep();

		U = new double[N + 1];
		V = new double[N + 1];
		p = new double[N];
		q = new double[N];

		alpha = new double[N + 1];
		beta = new double[N + 1];
		gamma = new double[N + 1];

		counts = (int) curve.getNumPoints().getValue();
		maxVal = 0;

		// coefficients for difference equation

		a = 1.0;
		c = 1.0;
	}

	@Override
	public void solve(DiathermicMaterialProblem problem) {

		prepare(problem);

		final double HX2_TAU = pow(hx, 2) / tau;

		// precalculated constants

		final double z0 = 1.0 + 0.5 * HX2_TAU + hx * Bi1 * (1.0 + eta);
		final double zN_1 = -hx * eta * Bi1;
		final double f01 = HX2_TAU / 2.0;
		final double fN1 = f01;

		double F;

		alpha[1] = 1.0 / z0;
		gamma[1] = -zN_1 / z0;

		int i, m, w;
		double pls;

		b = 2.0 + HX2_TAU;

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

				beta[1] = (hx * hx / (2.0 * tau) * U[0] + hx * pls) / z0;

				for (i = 1; i < N; i++) {
					alpha[i + 1] = c / (b - a * alpha[i]);
					F = -U[i] * HX2_TAU;
					beta[i + 1] = (a * beta[i] - F) / (b - a * alpha[i]);
					gamma[i + 1] = a * gamma[i] / (b - a * alpha[i]);
				}

				p[N - 1] = beta[N];
				q[N - 1] = alpha[N] + gamma[N];

				for (i = N - 2; i >= 0; i--) {
					p[i] = alpha[i + 1] * p[i + 1] + beta[i + 1];
					q[i] = alpha[i + 1] * q[i + 1] + gamma[i + 1];
				}

				V[N] = (fN1 * U[N] - zN_1 * p[0] + p[N - 1]) / (z0 + zN_1 * q[0] - q[N - 1]);

				for (i = N - 1; i >= 0; i--)
					V[i] = p[i] + V[N] * q[i];

				System.arraycopy(V, 0, U, 0, N + 1);

			}

			maxVal = Math.max(maxVal, V[N]);

			curve.addPoint((w * timeInterval) * tau * problem.timeFactor(), V[N]);

			/*
			 * UNCOMMENT TO DEBUG
			 */

			// debug(problem, V, w);

		}

		curve.scale(maxTemp / maxVal);

	}

	@Override
	public DifferenceScheme copy() {
		return new ImplicitDiathermicSolver(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
	}

	@Override
	public Class<? extends Problem> domain() {
		return DiathermicMaterialProblem.class;
	}

}