package pulse.problem.schemes.solvers;

import static java.lang.Math.max;
import static java.lang.Math.pow;

import pulse.problem.laser.DiscretePulse2D;
import pulse.problem.schemes.ADIScheme;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.Grid2D;
import pulse.problem.statements.LinearisedProblem2D;
import pulse.problem.statements.Problem;
import pulse.problem.statements.Problem2D;
import pulse.properties.NumericProperty;

/**
 * An alternating direction implicit (ADI) solver for a classical two-dimensional linearised problem.
 *
 */

public class ADILinearisedSolver extends ADIScheme implements Solver<LinearisedProblem2D> {

	private int N;
	private double hx;
	private double tau;
	private int firstIndex;
	private int lastIndex;

	private double d;
	private double l;
	private double Bi1;
	private double Bi2;
	private double Bi3;

	private double[][] U1;
	private double[][] U2; 
	private double[][] U1_E;
	private double[][] U2_E; 
	
	private double[] alpha;
	private double[] beta;
	private double[] a1;
	private double[] b1;
	private double[] c1;

	private final static double EPS = 1e-8;

	public ADILinearisedSolver() {
		super();
	}

	public ADILinearisedSolver(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
	}

	public ADILinearisedSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}

	private void prepare(Problem2D problem) {
		super.prepare(problem);

		var grid = getGrid();

		N = (int) grid.getGridDensity().getValue();
		hx = grid.getXStep();
		tau = grid.getTimeStep();

		Bi1 = (double) problem.getHeatLoss().getValue();
		Bi2 = Bi1;
		Bi3 = (double) problem.getSideLosses().getValue();

		d = (double) problem.getSampleDiameter().getValue();
		double fovOuter = (double) problem.getFOVOuter().getValue();
		double fovInner = (double) problem.getFOVInner().getValue();
		l = (double) problem.getSampleThickness().getValue();

		// end

		U1 = new double[N + 1][N + 1];
		U2 = new double[N + 1][N + 1];

		U1_E = new double[N + 3][N + 3];
		U2_E = new double[N + 3][N + 3];

		alpha = new double[N + 2];
		beta = new double[N + 2];

		a1 = new double[N + 1];
		b1 = new double[N + 1];
		c1 = new double[N + 1];

		// a[i]*u[i-1] - b[i]*u[i] + c[i]*u[i+1] = F[i]

		lastIndex = (int) (fovOuter / d / hx);
		lastIndex = lastIndex > N ? N : lastIndex;

		firstIndex = (int) (fovInner / d / hx);
		firstIndex = firstIndex < 0 ? 0 : firstIndex;
	}

	@Override
	public void solve(LinearisedProblem2D problem) {
		prepare(problem);
		var curve = problem.getHeatingCurve();
		final double maxTemp = (double) problem.getMaximumTemperature().getValue();
		
		final double hy = ((Grid2D) getGrid()).getYStep();
		final double HX2 = pow(hx, 2);
		final double HY2 = pow(hy, 2);
		
		// precalculated FD constants

		final double OMEGA = 2.0 * l / d;
		final double OMEGA_SQ = OMEGA * OMEGA;

		for (int i = 1; i < N + 1; i++) {
			a1[i] = OMEGA_SQ * (i - 0.5) / HX2 / i;
			b1[i] = 2. / tau + 2. * OMEGA_SQ / HX2;
			c1[i] = OMEGA_SQ * (i + 0.5) / HX2 / i;
		}

		final double a2 = 1. / HY2;
		final double b2 = 2. / HY2 + 2. / tau;
		final double c2 = 1. / HY2;

		// precalc coefs

		final double a11 = 1.0 / (1.0 + HX2 / (OMEGA_SQ * tau));
		final double b11 = 0.5 * tau / (1.0 + OMEGA_SQ * tau / HX2);

		final double _a11 = 1.0 / (1.0 + Bi1 * hy + HY2 / tau);
		final double _b11 = 1.0 / ((1 + hy * Bi1) * tau + HY2);
		final double _c11 = 0.5 * HY2 * tau * OMEGA_SQ / HX2;
		final double _b12 = _c11 * _b11;

		DiscretePulse2D discretePulse2D = (DiscretePulse2D) getDiscretePulse();
		final int timeInterval = getTimeInterval();
		
		double maxVal = 0;

		// begin time cycle

		for (int w = 1, counts = (int) curve.getNumPoints().getValue(); w < counts; w++) {

			for (int m = (w - 1) * timeInterval; m < w * timeInterval; m++) {

				/* create extended U1 array to accommodate edge values */

				for (int i = 0; i <= N; i++) {

					System.arraycopy(U1[i], 0, U1_E[i + 1], 1, N + 1);

					double pls = discretePulse2D.evaluateAt((m + EPS) * tau, i * hx); // i = 0, j = 0
					U1_E[i + 1][0] = U1[i][1] + 2.0 * hy * pls - 2.0 * hy * Bi1 * U1[i][0];
					U1_E[i + 1][N + 2] = U1[i][N - 1] - 2.0 * hy * Bi2 * U1[i][N];
				}

				// first equation, i -> x (radius), j -> y (thickness)

				alpha[1] = a11;

				for (int j = 0; j <= N; j++) {

					beta[1] = b11
							* (2. * U1_E[1][j + 1] / tau + (U1_E[1][j + 2] - 2. * U1_E[1][j + 1] + U1_E[1][j]) / HY2);

					for (int i = 1; i < N; i++) {
						double F = -2. * U1_E[i + 1][j + 1] / tau
								- (U1_E[i + 1][j] - 2.0 * U1_E[i + 1][j + 1] + U1_E[i + 1][j + 2]) / HY2;
						alpha[i + 1] = c1[i] / (b1[i] - a1[i] * alpha[i]);
						beta[i + 1] = (F - a1[i] * beta[i]) / (a1[i] * alpha[i] - b1[i]);
					}

					U2[N][j] = (OMEGA_SQ * tau * beta[N] + HX2 * U1_E[N + 1][j + 1]
							+ HX2 * tau / (2.0 * HY2) * (U1_E[N + 1][j + 2] - 2 * U1_E[N + 1][j + 1] + U1_E[N + 1][j]))
							/ ((1.0 - alpha[N] + hx * OMEGA * Bi3) * OMEGA_SQ * tau + HX2);

					for (int i = N - 1; i >= 0; i--) {
						U2[i][j] = alpha[i + 1] * U2[i + 1][j] + beta[i + 1];
					}

				}

				// second equation

				/* create extended U2 array to accommodate edge values */

				for (int j = 0; j <= N; j++) {

					for (int i = 0; i <= N; i++) {
						U2_E[i + 1][j + 1] = U2[i][j];
					}

					U2_E[N + 2][j + 1] = U2[N - 1][j] - 2.0 * hx * OMEGA * Bi3 * U2[N][j];
				}

				alpha[1] = _a11;

				for (int i = 1; i <= N; i++) {

					double pls = discretePulse2D.evaluateAt((m + 1 + EPS) * tau, i * hx);
					beta[1] = (tau * hy * pls + HY2 * U2_E[i + 1][1]) * _b11
							+ _b12 * (U2_E[i + 2][1] * (1 + 1.0 / (2.0 * i)) - 2. * U2_E[i + 1][1]
									+ (1 - 1.0 / (2.0 * i)) * U2_E[i][1]);

					for (int j = 1; j < N; j++) {
						double F = -2. / tau * U2_E[i + 1][j + 1]
								- OMEGA_SQ / HX2 * ((1 + 1.0 / (2.0 * i)) * U2_E[i + 2][j + 1] - 2. * U2_E[i + 1][j + 1]
										+ (1 - 1.0 / (2.0 * i)) * U2_E[i][j + 1]);
						alpha[j + 1] = c2 / (b2 - a2 * alpha[j]);
						beta[j + 1] = (F - a2 * beta[j]) / (a2 * alpha[j] - b2);
					}

					U1[i][N] = (tau * beta[N] + HY2 * U2_E[i + 1][N + 1]
							+ _c11 * ((1 + 1.0 / (2.0 * i)) * U2_E[i + 2][N + 1] - 2. * U2_E[i + 1][N + 1]
									+ (1 - 1.0 / (2.0 * i)) * U2_E[i][N + 1]))
							/ ((1 - alpha[N] + hy * Bi2) * tau + HY2);

					sweep(U1[i], alpha, beta);

				}

				// i = 0 boundary

				double pls = discretePulse2D.laserPowerAt((m + 1 + EPS) * tau);
				beta[1] = (tau * hy * pls + HY2 * U2_E[1][1]) * _b11 + 2.0 * _b12 * (U2_E[2][1] - U2_E[1][1]);

				for (int j = 1; j < N; j++) {
					double F = -2. / tau * U2_E[1][j + 1] - 2.0 * OMEGA_SQ / HX2 * (U2_E[2][j + 1] - U2_E[1][j + 1]);
					beta[j + 1] = (F - a2 * beta[j]) / (a2 * alpha[j] - b2);
				}

				U1[0][N] = (tau * beta[N] + HY2 * U2_E[1][N + 1] + 2.0 * _c11 * (U2_E[2][N + 1] - U2_E[1][N + 1]))
						/ ((1 - alpha[N] + hy * Bi2) * tau + HY2);

				sweep(U1[0], alpha, beta);

			}

			// calc average value

			double sum = 0;

			for (int i = firstIndex; i <= lastIndex; i++) {
				sum += U1[i][N];
			}

			sum /= (lastIndex - firstIndex + 1);

			curve.addPoint((w * timeInterval) * tau * problem.timeFactor(), sum);

			maxVal = max(maxVal, sum);

		}

		curve.scale(maxTemp / maxVal);

	}

	@Override
	public DifferenceScheme copy() {
		var grid = getGrid();
		return new ADILinearisedSolver(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
	}

	@Override
	public Class<? extends Problem> domain() {
		return LinearisedProblem2D.class;
	}

}