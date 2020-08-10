package pulse.problem.schemes.solvers;

import static java.lang.Math.pow;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericPropertyKeyword.GRID_DENSITY;
import static pulse.properties.NumericPropertyKeyword.TAU_FACTOR;
import static pulse.ui.Messages.getString;

import pulse.problem.schemes.CoupledScheme;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.statements.ParticipatingMedium;
import pulse.properties.NumericProperty;

public class ExplicitCoupledSolver extends CoupledScheme implements Solver<ParticipatingMedium> {

	private double[] U;
	private double[] V;
	
	private int N;
	private double hx;
	private double a;

	private final static double EPS = 1e-7; // a small value ensuring numeric stability

	public ExplicitCoupledSolver() {
		super( derive(GRID_DENSITY, 80), derive(TAU_FACTOR, 0.5) );
	}

	public ExplicitCoupledSolver(NumericProperty gridDensity, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(gridDensity, timeFactor, timeLimit);
	}

	private void prepare(ParticipatingMedium problem) {
		super.prepare(problem);

		var grid = getGrid();

		getCoupling().init(problem, grid);

		N = (int) grid.getGridDensity().getValue();
		hx = grid.getXStep();

		U = new double[N + 1];
		V = new double[N + 1];

		double Bi = (double) problem.getHeatLoss().getValue();

		a = 1. / (1. + Bi * hx);
	}

	@Override
	public void solve(ParticipatingMedium problem) throws SolverException {
		prepare(problem);
		var curve = problem.getHeatingCurve();
		var rte = getCoupling().getRadiativeTransferEquation();
		final var fluxes = rte.getFluxes();

		final double opticalThickness = (double) problem.getOpticalThickness().getValue();
		final double Np = (double) problem.getPlanckNumber().getValue();
		final double tau = getGrid().getTimeStep();
		
		final double TAU_HH = tau / pow(hx, 2);
		final double HX_NP = hx / Np;

		final double prefactor = tau * opticalThickness / Np;

		final double errorSq = pow((double)getNonlinearPrecision().getValue(), 2);

		double wFactor = getTimeInterval() * tau * problem.timeFactor();

		var status = rte.compute(U);

		final var discretePulse = getDiscretePulse();

		/*
		 * The outer cycle iterates over the number of points of the HeatingCurve
		 */

		for (int w = 1, counts = (int) curve.getNumPoints().getValue(); w < counts; w++) {

			/*
			 * Two adjacent points of the heating curves are separated by timeInterval on
			 * the time grid. Thus, to calculate the next point on the heating curve,
			 * timeInterval/tau time steps have to be made first.
			 */

			for (int m = (w - 1) * getTimeInterval() + 1; m < w * getTimeInterval() + 1
					&& status == RTECalculationStatus.NORMAL; m++) {

				/*
				 * Do the iterations
				 */

				/*
				 * Temperature at boundaries will strongly change the radiosities. This
				 * recalculates the latter using the solution at previous iteration
				 */

				for (double V_0 = Double.POSITIVE_INFINITY, V_N = Double.POSITIVE_INFINITY; (pow((V[0] - V_0),
						2) > errorSq) || (pow((V[N] - V_N), 2) > errorSq); status = rte.compute(V)) {

					/*
					 * Uses the heat equation explicitly to calculate the grid-function everywhere
					 * except the boundaries
					 */

					for (int i = 1; i < N; i++) {
						V[i] = U[i] + TAU_HH * (U[i + 1] - 2. * U[i] + U[i - 1]) + prefactor * fluxes.fluxDerivative(i);
					}

					/*
					 * Calculates boundary values
					 */

					double pls = discretePulse.laserPowerAt((m - EPS) * tau);

					// Front face
					V_0 = V[0];
					V[0] = (V[1] + hx * pls - HX_NP * fluxes.getFlux(0)) * a;
					// Rear face
					V_N = V[N];
					V[N] = (V[N - 1] + HX_NP * fluxes.getFlux(N)) * a;

				}

				System.arraycopy(V, 0, U, 0, N + 1);

			}

			curve.addPoint(w * wFactor, V[N]);

		}

		if (status != RTECalculationStatus.NORMAL)
			throw new SolverException(status.toString());

		final double maxTemp = (double) problem.getMaximumTemperature().getValue();
		curve.scale(maxTemp / curve.apparentMaximum());
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
	public DifferenceScheme copy() {
		var grid = getGrid();
		return new ExplicitCoupledSolver(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
	}

}