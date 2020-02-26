package pulse.problem.schemes.solvers;

import static java.lang.Math.pow;

import pulse.HeatingCurve;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.statements.NonlinearProblem;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;

public class ImplicitNonlinearSolver 
					extends ImplicitScheme 
							implements Solver<NonlinearProblem> {

	public ImplicitNonlinearSolver() {
		super();
	}

	public ImplicitNonlinearSolver(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
	}

	public ImplicitNonlinearSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}
	
	@Override
	public void solve(NonlinearProblem problem) {

		super.prepare(problem);

		int N		= (int)grid.getGridDensity().getValue();
		double hx	= grid.getXStep();
		double tau	= grid.getTimeStep();
		
		final double HH = pow(hx, 2);

		HeatingCurve curve = problem.getHeatingCurve();
		curve.reinit();
		final int counts = (int) curve.getNumPoints().getValue();

		final double Bi1 = (double) problem.getFrontHeatLoss().getValue();
		final double Bi2 = (double) problem.getHeatLossRear().getValue();
		final double fixedPointPrecisionSq = Math.pow((double) problem.getNonlinearPrecision().getValue(), 2);

		final double T = (double) problem.getTestTemperature().getValue();
		final double dT = problem.maximumHeating();

		double[] U = new double[N + 1];
		double[] V = new double[N + 1];
		double[] alpha = new double[N + 2];
		double[] beta = new double[N + 2];

		final double EPS = 1e-5;

		// constant for bc calc

		double a1 = 2. * tau / (HH + 2. * tau);
		double b1 = HH / (2. * tau + HH);
		double b2 = a1 * hx;
		double b3 = Bi1 * T / (4.0 * dT);
		double c1 = -0.5 * hx * tau * Bi2 * T / dT;
		double c2;

		int i, m, w, j;
		double F, pls;

		double a = 1. / pow(hx, 2);
		double b = 1. / tau + 2. / pow(hx, 2);
		double c = 1. / pow(hx, 2);

		// time cycle

		for (w = 1; w < counts; w++) {

			for (m = (w - 1) * timeInterval + 1; m < w * timeInterval + 1; m++) {

				pls = discretePulse.evaluateAt((m - EPS) * tau);
				alpha[1] = a1;

				for (i = 1; i < N; i++)
					alpha[i + 1] = c / (b - a * alpha[i]);

				c2 = 1. / (HH + 2. * tau - 2 * alpha[N] * tau);

				for (double lastIteration = Double.POSITIVE_INFINITY; pow((0.5 * (V[0] + V[N]) - lastIteration),
						2) > fixedPointPrecisionSq;) {

					lastIteration = 0.5 * (V[0] + V[N]);

					beta[1] = b1 * U[0] + b2 * (pls - b3 * (pow(V[0] * dT / T + 1, 4) - 1));

					for (i = 1; i < N; i++) {
						F = -U[i] / tau;
						beta[i + 1] = (F - a * beta[i]) / (a * alpha[i] - b);
					}

					V[N] = c2 * (2. * beta[N] * tau + HH * U[N]
							+ c1 * (pow(V[N] * dT / T + 1, 4) - 1));

					for (j = N - 1; j >= 0; j--)
						V[j] = alpha[j + 1] * V[j + 1] + beta[j + 1];

				}

				System.arraycopy(V, 0, U, 0, N + 1);

			}

			curve.addPoint(
					(w * timeInterval) * tau * problem.timeFactor(),
					V[N] );

			/*
			 * UNCOMMENT TO DEBUG
			 */

			//debug(problem, V, w);

		}

		curve.scale(dT);

	}
	
	@Override
	public DifferenceScheme copy() {
		return new ImplicitNonlinearSolver(grid.getGridDensity(),
				grid.getTimeFactor(), getTimeLimit());
	}
	
	@Override
	public Class<? extends Problem> domain() {
		return NonlinearProblem.class;
	}

}