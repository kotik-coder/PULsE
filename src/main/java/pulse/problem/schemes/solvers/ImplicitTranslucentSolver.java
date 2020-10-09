package pulse.problem.schemes.solvers;

import static pulse.problem.schemes.DistributedDetection.evaluateSignal;
import static pulse.problem.statements.model.SpectralRange.LASER;
import static pulse.ui.Messages.getString;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.Grid;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.schemes.TridiagonalMatrixAlgorithm;
import pulse.problem.statements.PenetrationProblem;
import pulse.problem.statements.Problem;
import pulse.problem.statements.model.AbsorptionModel;
import pulse.properties.NumericProperty;

public class ImplicitTranslucentSolver extends ImplicitScheme implements Solver<PenetrationProblem> {

	private AbsorptionModel absorption;
	private Grid grid;
	private double pls;
	private int N;

	private double HH;
	private double _2Bi1HTAU;
	private double b11;
	
	private double frontAbsorption;
	private double rearAbsorption;
	
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
		N = (int) grid.getGridDensity().getValue();

		final double Bi1H = (double) problem.getProperties().getHeatLoss().getValue() * grid.getXStep();
		final double hx = grid.getXStep();
		HH = hx * hx;
		_2Bi1HTAU = 2.0 * Bi1H * tau;
		b11 = 1.0 / (1.0 + 2.0 * tau / HH * (1 + Bi1H));

		absorption = problem.getAbsorptionModel();
		final double EPS = 1E-7;
		rearAbsorption = tau * absorption.absorption(LASER, (N - EPS) * hx);
		frontAbsorption = tau * absorption.absorption(LASER, 0.0);

		var tridiagonal = new TridiagonalMatrixAlgorithm(grid) {
			
			@Override
			public double phi(final int i) {
				return pls * absorption.absorption(LASER, (i - EPS) * hx);
			}
			
		};

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
		final double tau = grid.getTimeStep();

		return (HH * (getPreviousSolution()[N] + pls * rearAbsorption)
				+ 2. * tau * betaN) / (_2Bi1HTAU + HH + 2. * tau * (1 - alphaN));
	}

	@Override
	public double firstBeta(int m) {
		return (getPreviousSolution()[0] + pls * frontAbsorption) * b11;
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