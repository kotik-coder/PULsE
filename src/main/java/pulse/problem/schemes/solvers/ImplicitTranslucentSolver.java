package pulse.problem.schemes.solvers;

import static pulse.math.MathUtils.fastPowLoop;
import static pulse.problem.schemes.DistributedDetection.evaluateSignal;
import static pulse.problem.statements.penetration.AbsorptionModel.SpectralRange.LASER;
import static pulse.ui.Messages.getString;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.Grid;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.statements.PenetrationProblem;
import pulse.problem.statements.Problem;
import pulse.problem.statements.penetration.AbsorptionModel;
import pulse.properties.NumericProperty;

public class ImplicitTranslucentSolver extends ImplicitScheme implements Solver<PenetrationProblem> {

	private AbsorptionModel absorption;
	private Grid grid;
	private double pls;

	private double HH;
	private double Bi1H;

	private final static double EPS = 1e-7; // a small value ensuring numeric stability

	public ImplicitTranslucentSolver() {
		super();
	}

	public ImplicitTranslucentSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}

	private void prepare(PenetrationProblem problem) {
		super.prepare(problem);

		grid = getGrid();
		final double tau = grid.getTimeStep();

		Bi1H = (double) problem.getHeatLoss().getValue() * grid.getXStep();
		HH = fastPowLoop(grid.getXStep(), 2);

		absorption = problem.getAbsorptionModel();

		var tridiagonal = getTridiagonalMatrixAlgorithm();

		// coefficients for difference equation

		tridiagonal.setCoefA(1. / HH);
		tridiagonal.setCoefB(1. / tau + 2. / HH);
		tridiagonal.setCoefC(1. / HH);

		tridiagonal.setAlpha(1, 1.0 / (1.0 + HH / (2.0 * tau) + Bi1H));
		tridiagonal.evaluateAlpha();
		setTridiagonalMatrixAlgorithm(tridiagonal);
	}

	@Override
	public void solve(PenetrationProblem problem) {
		prepare(problem);
		runTimeSequence(problem);
	}

	@Override
	public void timeStep(final int m) {
		pls = pulse(m);
		super.timeStep(m);
	}

	@Override
	public double signal() {
		return evaluateSignal(absorption, grid, getCurrentSolution());
	}

	@Override
	public double evalRightBoundary(final int m, final double alphaN, final double betaN) {
		final int N = (int) grid.getGridDensity().getValue();
		final double tau = grid.getTimeStep();

		return (HH * (getPreviousSolution()[N] + tau * pls * absorption.absorption(LASER, (N - EPS) * grid.getXStep()))
				+ 2. * tau * betaN) / (2 * Bi1H * tau + HH + 2. * tau * (1 - alphaN));
	}

	@Override
	public double firstBeta(int m) {
		final double tau = grid.getTimeStep();
		return (getPreviousSolution()[0] + tau * pls * absorption.absorption(LASER, 0.0))
				/ (1.0 + 2.0 * tau / HH * (1 + Bi1H));
	}

	@Override
	public DifferenceScheme copy() {
		var grid = getGrid();
		return new ImplicitTranslucentSolver(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
	}

	/**
	 * Prints out the description of this problem type.
	 * 
	 * @return a verbose description of the problem.
	 */

	@Override
	public String toString() {
		return getString("ImplicitScheme.4");
	}

	@Override
	public Class<? extends Problem> domain() {
		return PenetrationProblem.class;
	}

}