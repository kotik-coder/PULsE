package pulse.problem.schemes.solvers;

import static pulse.properties.NumericPropertyKeyword.*;
import static pulse.ui.Messages.getString;
import static pulse.properties.NumericProperty.*;

import pulse.HeatingCurve;
import pulse.math.MathUtils;
import pulse.problem.schemes.CoupledScheme;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.statements.ParticipatingMedium;
import pulse.properties.NumericProperty;

public class ImplicitCoupledSolver extends CoupledScheme implements Solver<ParticipatingMedium> {

	private int N;
	private int counts;
	private double hx;
	private double tau;
	private double maxTemp;

	private HeatingCurve curve;

	private double[] U, V;
	private double[] alpha, beta;

	private double Np;

	private final static double EPS = 1e-7; // a small value ensuring numeric stability

	private double b11;
	private double a;
	private double b;
	private double c;

	private double HX2_2TAU;
	private double HX_2NP;

	private double v1;

	public ImplicitCoupledSolver() {
		super(derive(GRID_DENSITY, 20), derive(TAU_FACTOR, 0.66667));
	}

	public ImplicitCoupledSolver(NumericProperty gridDensity, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(gridDensity, timeFactor, timeLimit);
	}

	private void prepare(ParticipatingMedium problem) {
		super.prepare(problem);

		final var grid = getGrid();

		getCoupling().init(problem, grid);

		curve = problem.getHeatingCurve();

		N = (int) grid.getGridDensity().getValue();
		hx = grid.getXStep();
		tau = grid.getTimeStep();
		maxTemp = (double) problem.getMaximumTemperature().getValue();

		counts = (int) curve.getNumPoints().getValue();

		double Bi1 = (double) problem.getHeatLoss().getValue();
		double Bi2 = Bi1;
		Np = (double) problem.getPlanckNumber().getValue();

		U = new double[N + 1];
		V = new double[N + 1];
		alpha = new double[N + 2];
		beta = new double[N + 2];

		a = 1. / (hx * hx);
		b = 1. / tau + 2. / (hx * hx);
		c = 1. / (hx * hx);

		b11 = 1.0 / (2.0 * Np * hx);

		HX2_2TAU = hx * hx / (2.0 * tau);
		HX_2NP = hx / (2.0 * Np);

		alpha[1] = 1.0 / (1.0 + Bi1 * hx + HX2_2TAU);

		v1 = 1.0 + HX2_2TAU + hx * Bi2;

	}

	@Override
	public void solve(ParticipatingMedium problem) throws SolverException {

		prepare(problem);
		var rte = getCoupling().getRadiativeTransferEquation();
		final var fluxes = rte.getFluxes();

		final double errorSq = MathUtils.fastPowLoop((double)getNonlinearPrecision().getValue(), 2);

		int i, m, w, j;
		double F, pls;

		double V_0 = 0;
		double V_N = 0;

		double wFactor = getTimeInterval() * tau * problem.timeFactor();

		var status = rte.compute(U);

		for (i = 1; i < N; i++) {
			alpha[i + 1] = c / (b - a * alpha[i]);
		}

		final var discretePulse = getDiscretePulse();

		// time cycle

		for (w = 1; w < counts; w++) {

			for (m = (w - 1) * getTimeInterval() + 1; m < w * getTimeInterval() + 1
					&& status == RTECalculationStatus.NORMAL; m++) {

				pls = discretePulse.laserPowerAt((m - EPS) * tau);

				for (V_0 = errorSq + 1, V_N = errorSq + 1; (MathUtils.fastPowLoop((V[0] - V_0), 2) > errorSq)
						|| (MathUtils.fastPowLoop((V[N] - V_N), 2) > errorSq); status = rte.compute(V)) {

					beta[1] = (HX2_2TAU * U[0] + hx * pls - HX_2NP * (fluxes.getFlux(0) + fluxes.getFlux(1))) * alpha[1];

					for (i = 1; i < N; i++) {
						F = U[i] / tau + b11 * (fluxes.getFlux(i - 1) - fluxes.getFlux(i + 1));
						beta[i + 1] = (F + a * beta[i]) / (b - a * alpha[i]);
					}

					V_N = V[N];
					V[N] = (beta[N] + HX2_2TAU * U[N] + HX_2NP * (fluxes.getFlux(N - 1) + fluxes.getFlux(N)))
							/ (v1 - alpha[N]);

					V_0 = V[0];
					for (j = N - 1; j >= 0; j--) {
						V[j] = alpha[j + 1] * V[j + 1] + beta[j + 1];
					}

				}

				System.arraycopy(V, 0, U, 0, N + 1);

			}

			curve.addPoint(w * wFactor, V[N]);

			/*
			 * UNCOMMENT TO DEBUG
			 */

			// debug(problem, V, w);

		}

		if (status != RTECalculationStatus.NORMAL)
			throw new SolverException(status.toString());

		curve.scale(maxTemp / curve.apparentMaximum());

	}
	
	@Override
	public String toString() {
		return getString("ImplicitScheme.4");
	}

	@Override
	public DifferenceScheme copy() {
		var grid = getGrid();
		return new ImplicitCoupledSolver(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
	}

}