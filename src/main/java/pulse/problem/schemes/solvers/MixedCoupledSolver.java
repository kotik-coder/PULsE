package pulse.problem.schemes.solvers;

import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericProperty.theDefault;
import static pulse.properties.NumericPropertyKeyword.GRID_DENSITY;
import static pulse.properties.NumericPropertyKeyword.SCHEME_WEIGHT;
import static pulse.properties.NumericPropertyKeyword.TAU_FACTOR;

import java.util.List;

import pulse.math.MathUtils;
import pulse.problem.laser.DiscretePulse;
import pulse.problem.schemes.CoupledScheme;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.schemes.rte.RadiativeTransferSolver;
import pulse.problem.statements.ParticipatingMedium;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.ui.Messages;

public class MixedCoupledSolver extends CoupledScheme implements Solver<ParticipatingMedium> {

	private int N;
	private double hx;
	private double tau;

	private double sigma;

	private double[] U;
	private double[] V;
	private double[] alpha;
	private double[] beta;

	private final static double EPS = 1e-7; // a small value ensuring numeric stability

	private double a;
	private double b;
	private double c;

	private double Bi1;

	private double HX2;

	private double errorSq;

	private double HX_NP;
	private double TAU0_NP;
	private double Bi2HX;
	private double ONE_PLUS_Bi1_HX;
	private double SIGMA_NP;

	private double _2TAUHX;
	private double HX2_2TAU;
	private double ONE_MINUS_SIGMA_NP;
	private double _2TAU_ONE_MINUS_SIGMA;
	private double BETA1_FACTOR;
	private double ONE_MINUS_SIGMA;

	private final static NumericProperty TIMEFACTOR = derive(TAU_FACTOR, 0.25);

	public MixedCoupledSolver() {
		super(derive(GRID_DENSITY, 16), TIMEFACTOR);
		sigma = (double) theDefault(SCHEME_WEIGHT).getValue();
	}

	public MixedCoupledSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
		sigma = (double) theDefault(SCHEME_WEIGHT).getValue();
	}

	private void prepare(ParticipatingMedium problem) {
		super.prepare(problem);

		var grid = getGrid();

		getCoupling().init(problem, grid);

		N = (int) grid.getGridDensity().getValue();
		hx = grid.getXStep();

		Bi1 = (double) problem.getHeatLoss().getValue();

		U = new double[N + 1];
		V = new double[N + 1];
		beta = new double[N + 2];

		tau = grid.getTimeStep();
	}

	private void initAlpha() {
		a = sigma / HX2;
		b = 1. / tau + 2. * sigma / HX2;
		c = sigma / HX2;
		final double alpha0 = 1.0 / (HX2 / (2.0 * tau * sigma) + 1. + hx * Bi1);
		alpha = ImplicitScheme.alpha(getGrid(), alpha0, a, b, c);
	}

	private void initConst(ParticipatingMedium problem) {
		final double Np = (double) problem.getPlanckNumber().getValue();
		final double opticalThickness = (double) problem.getOpticalThickness().getValue();
		
		HX2 = hx * hx;
		_2TAUHX = 2.0 * tau * hx;
		HX2_2TAU = HX2 / (2.0 * tau);
		ONE_MINUS_SIGMA = 1.0 - sigma;
		ONE_MINUS_SIGMA_NP = ONE_MINUS_SIGMA / Np;
		_2TAU_ONE_MINUS_SIGMA = 2.0 * tau * ONE_MINUS_SIGMA;
		BETA1_FACTOR = 1.0 / (HX2 + 2.0 * tau * sigma * (1.0 + hx * Bi1));
		SIGMA_NP = sigma / Np;
		HX_NP = hx / Np;
		TAU0_NP = opticalThickness / Np;
		Bi2HX = Bi1 * hx;
		ONE_PLUS_Bi1_HX = (1. + Bi1 * hx);
	}

	@Override
	public void solve(ParticipatingMedium problem) throws SolverException {
		prepare(problem);
		var curve = problem.getHeatingCurve();
		adjustSchemeWeight();

		double wFactor = getTimeInterval() * tau * problem.timeFactor();
		errorSq = MathUtils.fastPowLoop( (double)super.getNonlinearPrecision().getValue(), 2);

		initConst(problem);
		initAlpha();

		int pulseEnd = (int) Math.rint(this.getDiscretePulse().getDiscretePulseWidth() / wFactor) + 1;
		int w;
		
		var status = getCoupling().getRadiativeTransferEquation().compute(U);

		final var discretePulse = getDiscretePulse();

		// time cycle

		for (w = 1; w < pulseEnd + 1 && status == RTECalculationStatus.NORMAL; w++) {

			for (int m = (w - 1) * getTimeInterval() + 1; m < w * getTimeInterval() + 1; m++) {
				status = timeStep(discretePulse, m, true);
			}
			curve.addPoint(w * wFactor, V[N]);
		}

		double timeLeft = (double) getTimeLimit().getValue() - (w - 1) * wFactor;

		var grid = getGrid();

		// adjust timestep to make calculations faster
		grid.setTimeFactor(TIMEFACTOR);

		tau = grid.getTimeStep();
		adjustSchemeWeight();

		initConst(problem);
		initAlpha();

		final int counts = (int) curve.getNumPoints().getValue();
		double numPoints = counts - (w - 1);
		double dt = timeLeft / (problem.timeFactor() * (numPoints - 1));

		setTimeInterval((int) (dt / tau) + 1);

		for (wFactor = getTimeInterval() * tau * problem.timeFactor(); w < counts
				&& status == RTECalculationStatus.NORMAL; w++) {

			for (int m = (w - 1) * getTimeInterval() + 1; m < w * getTimeInterval() + 1; m++) {
				status = timeStep(discretePulse, m, false);
			}

			curve.addPoint(w * wFactor, V[N]);
		}

		if (status != RTECalculationStatus.NORMAL)
			throw new SolverException(status.toString());

		final double maxTemp = (double) problem.getMaximumTemperature().getValue();
		curve.scale(maxTemp / curve.apparentMaximum());

	}

	private RTECalculationStatus timeStep(DiscretePulse discretePulse, final int m, final boolean activePulse) {
		var rte = getCoupling().getRadiativeTransferEquation();
		var grid = getGrid();
		final var fluxes = rte.getFluxes();

		double pls = activePulse
				? (discretePulse.laserPowerAt((m - 1 + EPS) * tau) * ONE_MINUS_SIGMA
						+ discretePulse.laserPowerAt((m - EPS) * tau) * sigma)
				: 0.0;

		RTECalculationStatus status = RTECalculationStatus.NORMAL;

		for (double V_0 = errorSq + 1, V_N = errorSq + 1; ((MathUtils.fastPowLoop((V[0] - V_0), 2) > errorSq)
				|| (MathUtils.fastPowLoop((V[N] - V_N), 2) > errorSq))
				&& status == RTECalculationStatus.NORMAL; status = rte.compute(V)) {

			// i = 0
			double phi = TAU0_NP * fluxes.fluxDerivativeFront();
			beta[1] = (_2TAUHX * (pls - SIGMA_NP * fluxes.getFlux(0) - ONE_MINUS_SIGMA_NP * fluxes.getStoredFlux(0))
					+ HX2 * (U[0] + phi * tau) + _2TAU_ONE_MINUS_SIGMA * (U[1] - U[0] * ONE_PLUS_Bi1_HX))
					* BETA1_FACTOR;

			// i = 1
			phi = TAU0_NP * phiNextToFront(rte);
			double F = U[1] / tau + phi + ONE_MINUS_SIGMA * (U[2] - 2 * U[1] + U[0]) / HX2;
			beta[2] = (F + a * beta[1]) / (b - a * alpha[1]);

			for (int i = 2; i < N - 1; i++) {
				phi = TAU0_NP * phi(rte, i);
				F = U[i] / tau + phi + ONE_MINUS_SIGMA * (U[i + 1] - 2 * U[i] + U[i - 1]) / HX2;
				beta[i + 1] = (F + a * beta[i]) / (b - a * alpha[i]);
			}

			// i = N - 1

			phi = TAU0_NP * phiNextToRear(rte);
			F = U[N - 1] / tau + phi + ONE_MINUS_SIGMA * (U[N] - 2 * U[N - 1] + U[N - 2]) / HX2;
			beta[N] = (F + a * beta[N - 1]) / (b - a * alpha[N - 1]);

			V_N = V[N];
			phi = TAU0_NP * fluxes.fluxDerivativeRear();
			V[N] = (sigma * beta[N] + HX2_2TAU * U[N] + 0.5 * HX2 * phi
					+ ONE_MINUS_SIGMA * (U[N - 1] - U[N] * (1. + hx * Bi1))
					+ HX_NP * (sigma * fluxes.getFlux(N) + ONE_MINUS_SIGMA * fluxes.getStoredFlux(N)))
					/ (HX2_2TAU + sigma * (1. - alpha[N] + Bi2HX));

			V_0 = V[0];
			ImplicitScheme.sweep(grid, V, alpha, beta);

		}

		System.arraycopy(V, 0, U, 0, N + 1);
		fluxes.store();
		return status;

	}

	private void adjustSchemeWeight() {
		final double newSigma = 0.5 - hx * hx / (12.0 * tau);
		setWeight(derive(SCHEME_WEIGHT, newSigma > 0 ? newSigma : 0.5));
	}

	private double phiNextToFront(RadiativeTransferSolver rte) {
		final var fluxes = rte.getFluxes();
		return 0.833333333 * fluxes.meanFluxDerivative(1)
				+ 0.083333333 * (fluxes.meanFluxDerivativeFront() + fluxes.meanFluxDerivative(2));
	}

	private double phiNextToRear(RadiativeTransferSolver rte) {
		final var fluxes = rte.getFluxes();
		return 0.833333333 * fluxes.meanFluxDerivative(N - 1)
				+ 0.083333333 * (fluxes.meanFluxDerivative(N - 2) + fluxes.meanFluxDerivativeveRear());
	}

	private double phi(RadiativeTransferSolver rte, int i) {
		final var fluxes = rte.getFluxes();
		return 0.833333333 * fluxes.meanFluxDerivative(i)
				+ 0.083333333 * (fluxes.meanFluxDerivative(i - 1) + fluxes.meanFluxDerivative(i + 1));
	}

	public void setWeight(NumericProperty weight) {
		requireType(weight, SCHEME_WEIGHT);
		this.sigma = (double) weight.getValue();
	}

	public NumericProperty getWeight() {
		return derive(SCHEME_WEIGHT, sigma);
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(def(SCHEME_WEIGHT));
		return list;
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		if(type == SCHEME_WEIGHT) 
			setWeight(property);
		else
			super.set(type, property);
	}

	@Override
	public String toString() {
		return Messages.getString("MixedScheme2.4");
	}

	@Override
	public DifferenceScheme copy() {
		var grid = getGrid();
		return new MixedCoupledSolver(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
	}

}