package pulse.problem.schemes.solvers;

import static pulse.properties.NumericPropertyKeyword.NONLINEAR_PRECISION;

import java.util.List;

import pulse.HeatingCurve;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.Grid;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.schemes.rte.MathUtils;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.schemes.rte.RadiativeTransferSolver;
import pulse.problem.schemes.rte.exact.NonscatteringDiscreteDerivatives;
import pulse.problem.statements.ParticipatingMedium;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.InstanceDescriptor;

public class ImplicitCoupledSolver extends ImplicitScheme implements Solver<ParticipatingMedium> {

	/**
	 * The default value of {@code tauFactor}, which is set to {@code 0.66667} for
	 * this scheme.
	 */

	public final static NumericProperty TAU_FACTOR = NumericProperty.derive(NumericPropertyKeyword.TAU_FACTOR, 0.66667);

	/**
	 * The default value of {@code gridDensity}, which is set to {@code 20} for this
	 * scheme.
	 */

	public final static NumericProperty GRID_DENSITY = NumericProperty.derive(NumericPropertyKeyword.GRID_DENSITY, 20);

	private int N;
	private int counts;
	private double hx;
	private double tau;
	private double maxTemp;

	private HeatingCurve curve;

	private double[] U, V;
	private double[] alpha, beta;

	private RadiativeTransferSolver rte;

	private double Np;

	private final static double EPS = 1e-7; // a small value ensuring numeric stability

	private double b11;
	private double a;
	private double b;
	private double c;

	private double HX2_2TAU;
	private double HX_2NP;

	private double v1;

	private double nonlinearPrecision = (double) NumericProperty.def(NONLINEAR_PRECISION).getValue();

	private static InstanceDescriptor<? extends RadiativeTransferSolver> instanceDescriptor = new InstanceDescriptor<RadiativeTransferSolver>(
			"RTE Solver Selector", RadiativeTransferSolver.class);

	static {
		instanceDescriptor.setSelectedDescriptor(NonscatteringDiscreteDerivatives.class.getSimpleName());
	}

	public ImplicitCoupledSolver() {
		this(GRID_DENSITY, TAU_FACTOR);
	}

	public ImplicitCoupledSolver(NumericProperty N, NumericProperty timeFactor) {
		super(GRID_DENSITY, TAU_FACTOR);
	}

	public ImplicitCoupledSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		this(N, timeFactor);
		setTimeLimit(timeLimit);
	}

	private void prepare(ParticipatingMedium problem) {
		super.prepare(problem);

		initRTE(problem, grid);

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

		final double errorSq = MathUtils.fastPowLoop(nonlinearPrecision, 2);

		int i, m, w, j;
		double F, pls;

		double V_0 = 0;
		double V_N = 0;

		double wFactor = timeInterval * tau * problem.timeFactor();

		var status = rte.compute(U);

		for (i = 1; i < N; i++) {
                    alpha[i + 1] = c / (b - a * alpha[i]);
                }

		// time cycle

		for (w = 1; w < counts; w++) {

			for (m = (w - 1) * timeInterval + 1; m < w * timeInterval + 1
					&& status == RTECalculationStatus.NORMAL; m++) {

				pls = discretePulse.evaluateAt((m - EPS) * tau);

				for (V_0 = errorSq + 1, V_N = errorSq + 1; (MathUtils.fastPowLoop((V[0] - V_0), 2) > errorSq)
						|| (MathUtils.fastPowLoop((V[N] - V_N), 2) > errorSq); status = rte.compute(V)) {

					beta[1] = (HX2_2TAU * U[0] + hx * pls - HX_2NP * (rte.getFlux(0) + rte.getFlux(1))) * alpha[1];

					for (i = 1; i < N; i++) {
						F = U[i] / tau + b11 * (rte.getFlux(i - 1) - rte.getFlux(i + 1));
						beta[i + 1] = (F + a * beta[i]) / (b - a * alpha[i]);
					}

					V_N = V[N];
					V[N] = (beta[N] + HX2_2TAU * U[N] + HX_2NP * (rte.getFlux(N - 1) + rte.getFlux(N)))
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
	public DifferenceScheme copy() {
		return new ImplicitCoupledSolver(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
	}

	@Override
	public Class<? extends Problem> domain() {
		return ParticipatingMedium.class;
	}

	public RadiativeTransferSolver getRadiativeTransferEquation() {
		return rte;
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
		list.add(instanceDescriptor);
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

	public static InstanceDescriptor<? extends RadiativeTransferSolver> getInstanceDescriptor() {
		return instanceDescriptor;
	}

	public static void setInstanceDescriptor(InstanceDescriptor<? extends RadiativeTransferSolver> instanceDescriptor) {
		ImplicitCoupledSolver.instanceDescriptor = instanceDescriptor;
	}

	private void initRTE(ParticipatingMedium problem, Grid grid) {

		if (rte == null) {
			newRTE(problem, grid);
			instanceDescriptor.addListener(() -> {
				newRTE(problem, grid);
				rte.init(problem, grid);
			});

		}

		rte.init(problem, grid);

	}

	private void newRTE(ParticipatingMedium problem, Grid grid) {
		rte = instanceDescriptor.newInstance(RadiativeTransferSolver.class, problem, grid);
		rte.setParent(this);
	}

}