package pulse.problem.schemes.solvers;

import static pulse.math.MathUtils.fastPowLoop;
import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericPropertyKeyword.NONLINEAR_PRECISION;

import java.util.List;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ExplicitScheme;
import pulse.problem.schemes.FixedPointIterations;
import pulse.problem.statements.NonlinearProblem;
import pulse.problem.statements.Problem;
import pulse.problem.statements.Pulse2D;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public class ExplicitNonlinearSolver extends ExplicitScheme implements Solver<NonlinearProblem>, FixedPointIterations {

	private int N;
	private double hx;
	private double pls;

	private double dT_T;
	
	private double a00;
	private double a11;
	private double f01;
	private double fN1;

	private double nonlinearPrecision;

	public ExplicitNonlinearSolver() {
		super();
		nonlinearPrecision = (double) def(NONLINEAR_PRECISION).getValue();
	}

	public ExplicitNonlinearSolver(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
		nonlinearPrecision = (double) def(NONLINEAR_PRECISION).getValue();
	}

	public ExplicitNonlinearSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
		nonlinearPrecision = (double) def(NONLINEAR_PRECISION).getValue();
	}

	private void prepare(NonlinearProblem problem) {
		super.prepare(problem);

		var grid = getGrid();

		N = (int) grid.getGridDensity().getValue();
		hx = grid.getXStep();
		final double tau = grid.getTimeStep();

		var p = problem.getProperties();
		final double T = (double) p.getTestTemperature().getValue();
		final double dT = p.maximumHeating((Pulse2D)problem.getPulse());
		
		a00 = 2 * tau / (hx * hx + 2 * tau);
		a11 = hx * hx / (2.0 * tau);
		final double Bi1 = (double) p.getHeatLoss().getValue();
		f01 = 0.25 * Bi1 * T / dT;
		fN1 = 0.25 * Bi1 * T / dT;
		
		dT_T = dT/T;
	}
	
	@Override
	public void timeStep(int m) {
		explicitSolution();
		pls = pulse(m);
		doIterations(getCurrentSolution(), nonlinearPrecision, m);	
	}
	
	@Override
	public void iteration(int m) {
		var V = getCurrentSolution();
		var U = getPreviousSolution();
		
		/**
		 * y = 0
		 */
		
		final double f0 = f01 * (fastPowLoop(V[0] * dT_T + 1, 4) - 1);
		V[0] = a00 * (V[1] + a11 * U[0] + hx * (pls - f0));
		
		/**
		 * y = 1
		 */
		
		final double fN = fN1 * (fastPowLoop(V[N] * dT_T  + 1, 4) - 1);
		V[N] = a00 * (V[N - 1] + a11 * U[N] - hx * fN);
	}

	@Override
	public void solve(NonlinearProblem problem) {
		prepare(problem);
		runTimeSequence(problem);
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
		super.set(type, property);
		
		if(type == NONLINEAR_PRECISION)
			setNonlinearPrecision(property);
		else
			throw new IllegalArgumentException("Property not recognised: " + property);
	}

}