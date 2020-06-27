package pulse.problem.schemes.solvers;

import static java.lang.Math.pow;
import static pulse.properties.NumericPropertyKeyword.NONLINEAR_PRECISION;

import java.util.List;

import pulse.HeatingCurve;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.statements.NonlinearProblem;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public class ImplicitNonlinearSolver extends ImplicitScheme implements Solver<NonlinearProblem> {

	private int N;
	private int counts;
	private double hx;
	private double tau;

	private HeatingCurve curve;

	private double[] U, V;
	private double[] alpha, beta;

	private final static double EPS = 1e-7; // a small value ensuring numeric stability

	private double T, dT;

	private double a1, b1, c1, b2, b3, a, b, c;

	private double nonlinearPrecision = (double) NumericProperty.def(NONLINEAR_PRECISION).getValue();

	public ImplicitNonlinearSolver() {
		super();
	}

	public ImplicitNonlinearSolver(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
	}

	public ImplicitNonlinearSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}

	private void prepare(NonlinearProblem problem) {
		super.prepare(problem);
		curve = problem.getHeatingCurve();

		N = (int) grid.getGridDensity().getValue();
		hx = grid.getXStep();
		tau = grid.getTimeStep();

		final double HH = pow(hx, 2);

		counts = (int) curve.getNumPoints().getValue();

		double Bi1 = (double) problem.getHeatLoss().getValue();
		double Bi2 = Bi1;
		T = (double) problem.getTestTemperature().getValue();
		dT = problem.maximumHeating();

		U = new double[N + 1];
		V = new double[N + 1];
		alpha = new double[N + 2];
		beta = new double[N + 2];

		// constant for bc calc

		a1 = 2. * tau / (HH + 2. * tau);
		b1 = HH / (2. * tau + HH);
		b2 = a1 * hx;
		b3 = Bi1 * T / (4.0 * dT);
		c1 = -0.5 * hx * tau * Bi2 * T / dT;

		a = 1. / pow(hx, 2);
		b = 1. / tau + 2. / pow(hx, 2);
		c = 1. / pow(hx, 2);
	}

	@Override
	public void solve(NonlinearProblem problem) {

		prepare(problem);

		final double fixedPointPrecisionSq = pow(nonlinearPrecision, 2);
		final double HH = pow(hx, 2);

		int i, m, w, j;
		double F, pls;
		double c2;

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

					V[N] = c2 * (2. * beta[N] * tau + HH * U[N] + c1 * (pow(V[N] * dT / T + 1, 4) - 1));

					for (j = N - 1; j >= 0; j--)
						V[j] = alpha[j + 1] * V[j + 1] + beta[j + 1];

				}

				System.arraycopy(V, 0, U, 0, N + 1);

			}

			curve.addPoint((w * timeInterval) * tau * problem.timeFactor(), V[N]);

			/*
			 * UNCOMMENT TO DEBUG
			 */

			// debug(problem, V, w);

		}

		curve.scale(dT);

	}

	@Override
	public DifferenceScheme copy() {
		return new ImplicitNonlinearSolver(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
	}

	@Override
	public Class<? extends Problem> domain() {
		return NonlinearProblem.class;
	}

	public NumericProperty getNonlinearPrecision() {
		return NumericProperty.derive(NONLINEAR_PRECISION, nonlinearPrecision);
	}

	public void setNonlinearPrecision(NumericProperty nonlinearPrecision) {
		this.nonlinearPrecision = (double) nonlinearPrecision.getValue();
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(NumericProperty.def(NumericPropertyKeyword.NONLINEAR_PRECISION));
		return list;
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch (type) {
		case NONLINEAR_PRECISION:
			setNonlinearPrecision(property);
			break;
		default:
			throw new IllegalArgumentException("Property not recognised: " + property);
		}
	}

}