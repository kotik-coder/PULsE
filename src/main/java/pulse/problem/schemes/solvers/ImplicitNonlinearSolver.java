package pulse.problem.schemes.solvers;

import static pulse.math.MathUtils.fastPowLoop;
import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericPropertyKeyword.NONLINEAR_PRECISION;

import java.util.List;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.statements.NonlinearProblem;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public class ImplicitNonlinearSolver extends ImplicitScheme implements Solver<NonlinearProblem> {

	private int N;
	private double HH;
	private double tau;
	private double pls;
	
	private double dT_T;

	private double b1;
	private double c1;
	private double c2;
	private double b2;
	private double b3;

	private double nonlinearPrecision;

	public ImplicitNonlinearSolver() {
		super();
		nonlinearPrecision = (double) def(NONLINEAR_PRECISION).getValue();
	}

	public ImplicitNonlinearSolver(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
		nonlinearPrecision = (double) def(NONLINEAR_PRECISION).getValue();
	}

	public ImplicitNonlinearSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}

	private void prepare(NonlinearProblem problem) {
		super.prepare(problem);

		var grid = getGrid();

		N = (int) grid.getGridDensity().getValue();
		final double hx = grid.getXStep();
		tau = grid.getTimeStep();

		HH = hx*hx;
		final double Bi1 = (double) problem.getHeatLoss().getValue();

		final double T = (double) problem.getTestTemperature().getValue();
		final double dT = problem.maximumHeating();
		dT_T = dT/T;

		// constant for bc calc

		final double a1 = 2. * tau / (HH + 2. * tau);
		b1 = HH / (2. * tau + HH);
		b2 = a1 * hx;
		b3 = Bi1 * T / (4.0 * dT);
		c1 = -0.5 * hx * tau * Bi1 * T / dT;

		var tridiagonal = getTridiagonalMatrixAlgorithm();
		
		tridiagonal.setCoefA(  1.0/HH);
		tridiagonal.setCoefB(  1.0/tau + 2.0/HH);
		tridiagonal.setCoefC(  1.0/HH);
	
		tridiagonal.setAlpha(1, a1);
		tridiagonal.evaluateAlpha();
		c2 = 1. / (HH + 2. * tau - 2 * tridiagonal.getAlpha()[N] * tau);
	}

	@Override
	public void solve(NonlinearProblem problem) {
		prepare(problem);
		runTimeSequence(problem);
	}

	@Override
	public DifferenceScheme copy() {
		var grid = getGrid();
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

	@Override
	public void timeStep(final int m) {
		pls = pulse(m);
		final double errorSq = fastPowLoop((double) getNonlinearPrecision().getValue(), 2);
		
		var V = getCurrentSolution();
		
		for (double V_0 = errorSq + 1, V_N = errorSq + 1; (fastPowLoop((V[0] - V_0), 2) > errorSq)
				|| (fastPowLoop((V[N] - V_N), 2) > errorSq); ) {

			V_N = V[N];
			V_0 = V[0];
			super.timeStep(m);

		}	
		
	}
	
	@Override
	public double evalRightBoundary(int m, double alphaN, double betaN) {
		return c2 * (2. * betaN * tau + HH * getPreviousSolution()[N] + c1 * (fastPowLoop(getCurrentSolution()[N] * dT_T + 1, 4) - 1));
	}

	@Override
	public double firstBeta(int m) {
		return b1 * getPreviousSolution()[0] + b2 * (pls - b3 * (fastPowLoop(getCurrentSolution()[0] * dT_T + 1, 4) - 1));
	}

}