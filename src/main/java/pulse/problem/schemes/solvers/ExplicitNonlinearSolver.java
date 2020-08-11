package pulse.problem.schemes.solvers;

import static java.lang.Math.pow;
import static pulse.properties.NumericPropertyKeyword.NONLINEAR_PRECISION;

import java.util.List;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ExplicitScheme;
import pulse.problem.statements.NonlinearProblem;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public class ExplicitNonlinearSolver extends ExplicitScheme implements Solver<NonlinearProblem> {

	private int N;
	private double hx;
	private double tau;

	private double[] U;
	private double[] V;

	private final static double EPS = 1e-7; // a small value ensuring numeric stability

	private double T;
	private double dT;

	private double nonlinearPrecision = (double) NumericProperty.def(NONLINEAR_PRECISION).getValue();

	public ExplicitNonlinearSolver() {
		super();
	}

	public ExplicitNonlinearSolver(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
	}

	public ExplicitNonlinearSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}

	private void prepare(NonlinearProblem problem) {
		super.prepare(problem);

		var grid = getGrid();

		N = (int) grid.getGridDensity().getValue();
		hx = grid.getXStep();
		tau = grid.getTimeStep();
		
		U = new double[N + 1];
		V = new double[N + 1];

		T = (double) problem.getTestTemperature().getValue();
		dT = problem.maximumHeating();
	}

	@Override
	public void solve(NonlinearProblem problem) {
		prepare(problem);
		
		var curve = problem.getHeatingCurve();
		var grid = getGrid();

		final double a00 = 2 * tau / (hx * hx + 2 * tau);
		final double a11 = hx * hx / (2.0 * tau);
		final double Bi1 = (double) problem.getHeatLoss().getValue();
		final double f01 = 0.25 * Bi1 * T / dT;
		final double fN1 = 0.25 * Bi1 * T / dT;
		double f0;
		double fN;

		final double fixedPointPrecisionSq = pow(nonlinearPrecision, 2);

		final var discretePulse = getDiscretePulse();

		for (int w = 1, counts = (int) curve.getNumPoints().getValue(); w < counts; w++) {

			for (int m = (w - 1) * getTimeInterval() + 1; m < w * getTimeInterval() + 1; m++) {

				explicitSolution(grid, V, U);

				double pls = discretePulse.laserPowerAt((m - EPS) * tau);

				/**
				 * y = 0
				 */

				for (double lastIteration = Double.POSITIVE_INFINITY; pow((V[0] - lastIteration),
						2) > fixedPointPrecisionSq;) {
					lastIteration = V[0];
					f0 = f01 * (pow(lastIteration * dT / T + 1, 4) - 1);
					V[0] = a00 * (V[1] + a11 * U[0] + hx * (pls - f0));
				}

				/**
				 * y = 1
				 */

				for (double lastIteration = Double.POSITIVE_INFINITY; pow((V[N] - lastIteration),
						2) > fixedPointPrecisionSq;) {
					lastIteration = V[N];
					fN = fN1 * (pow(lastIteration * dT / T + 1, 4) - 1);
					V[N] = a00 * (V[N - 1] + a11 * U[N] - hx * fN);
				}

				System.arraycopy(V, 0, U, 0, N + 1);

			}

			curve.addPoint((w * getTimeInterval()) * tau * problem.timeFactor(), V[N]);

		}

		curve.scale(dT);

	}

	@Override
	public DifferenceScheme copy() {
		var grid = getGrid();
		return new ExplicitNonlinearSolver(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
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