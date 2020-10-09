package pulse.problem.schemes.solvers;

import static pulse.problem.schemes.DistributedDetection.evaluateSignal;
import static pulse.problem.statements.model.SpectralRange.LASER;
import static pulse.ui.Messages.getString;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ExplicitScheme;
import pulse.problem.statements.PenetrationProblem;
import pulse.problem.statements.Problem;
import pulse.problem.statements.model.AbsorptionModel;
import pulse.properties.NumericProperty;

public class ExplicitTranslucentSolver extends ExplicitScheme implements Solver<PenetrationProblem> {

	private int N;
	private double hx;
	private double tau;
	private double a;

	private double pls;

	private final static double EPS = 1e-7; // a small value ensuring numeric stability

	private AbsorptionModel model;

	public ExplicitTranslucentSolver() {
		super();
	}

	public ExplicitTranslucentSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}

	private void prepare(PenetrationProblem problem) {
		super.prepare(problem);

		var grid = getGrid();
		model = problem.getAbsorptionModel();

		N = (int) grid.getGridDensity().getValue();
		hx = grid.getXStep();
		tau = grid.getTimeStep();

		final double Bi1 = (double) problem.getProperties().getHeatLoss().getValue();
		a = 1. / (1. + Bi1 * hx);
	}

	@Override
	public void solve(PenetrationProblem problem) throws SolverException {
		this.prepare(problem);
		runTimeSequence(problem);
	}

	@Override
	public void timeStep(final int m) {
		pls = this.pulse(m);

		/*
		 * Uses the heat equation explicitly to calculate the grid-function everywhere
		 * except the boundaries
		 */
		explicitSolution();

		/*
		 * Calculates boundary values
		 */

		var V = getCurrentSolution();
		setSolutionAt(0, V[1] * a);
		setSolutionAt(N, V[N - 1] * a);

	}
	
	@Override
	public double phi(final int i) {
		return tau * pls * model.absorption(LASER, (i - EPS) * hx);
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

	@Override
	public double signal() {
		return evaluateSignal(model, getGrid(), getCurrentSolution());
	}

}