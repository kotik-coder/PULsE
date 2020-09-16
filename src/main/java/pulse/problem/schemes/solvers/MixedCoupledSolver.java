package pulse.problem.schemes.solvers;

import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.GRID_DENSITY;
import static pulse.properties.NumericPropertyKeyword.SCHEME_WEIGHT;
import static pulse.properties.NumericPropertyKeyword.TAU_FACTOR;

import java.util.List;

import pulse.problem.schemes.CoupledImplicitScheme;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.TridiagonalMatrixAlgorithm;
import pulse.problem.schemes.rte.Fluxes;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.schemes.rte.RadiativeTransferSolver;
import pulse.problem.statements.ParticipatingMedium;
import pulse.problem.statements.ThermoOpticalProperties;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.ui.Messages;

public class MixedCoupledSolver extends CoupledImplicitScheme implements Solver<ParticipatingMedium> {

	private RadiativeTransferSolver rte;
	private Fluxes fluxes;

	private int N;
	private double hx;
	private double tau;
	private double sigma;

	private final static double A = 5.0 / 6.0;
	private final static double B = 1.0 / 12.0;

	private final static double EPS = 1e-7; // a small value ensuring numeric stability

	private double Bi1;

	private double HX2;
	private double HX_NP;
	private double TAU0_NP;
	private double ONE_PLUS_Bi1_HX;
	private double SIGMA_NP;

	private double _2TAUHX;
	private double HX2_2TAU;
	private double ONE_MINUS_SIGMA_NP;
	private double _2TAU_ONE_MINUS_SIGMA;
	private double BETA1_FACTOR;
	private double ONE_MINUS_SIGMA;

	public MixedCoupledSolver() {
		super(derive(GRID_DENSITY, 16), derive(TAU_FACTOR, 0.25));
		sigma = (double) def(SCHEME_WEIGHT).getValue();
	}

	public MixedCoupledSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
		sigma = (double) def(SCHEME_WEIGHT).getValue();
	}

	private void prepare(ParticipatingMedium problem) {
		super.prepare(problem);

		var grid = getGrid();

		var coupling = getCoupling();
		coupling.init(problem, grid);
		rte = coupling.getRadiativeTransferEquation();

		var U = getPreviousSolution();

		N = (int) grid.getGridDensity().getValue();
		hx = grid.getXStep();
		tau = grid.getTimeStep();

		Bi1 = (double) problem.getProperties().getHeatLoss().getValue();

		fluxes = rte.getFluxes();

		var tridiagonal = new TridiagonalMatrixAlgorithm(grid) {

			@Override
			public double phi(int i) {
				return A * fluxes.meanFluxDerivative(i)
						+ B * (fluxes.meanFluxDerivative(i - 1) + fluxes.meanFluxDerivative(i + 1));
			}

			@Override
			public double beta(final double f, final double phi, final int i) {
				return super.beta(f + ONE_MINUS_SIGMA * (U[i] - 2.0 * U[i - 1] + U[i - 2]) / HX2, TAU0_NP * phi, i);
			}

			@Override
			public void evaluateBeta(final double[] U) {
				final double phiSecond = A * fluxes.meanFluxDerivative(1)
						+ B * (fluxes.meanFluxDerivativeFront() + fluxes.meanFluxDerivative(2));
				setBeta(2, beta(U[1] / tau, phiSecond, 2));

				super.evaluateBeta(U, 3, N);

				final double phiLast = A * fluxes.meanFluxDerivative(N - 1)
						+ B * (fluxes.meanFluxDerivative(N - 2) + fluxes.meanFluxDerivativeRear());
				setBeta(N, beta(U[N - 1] / tau, phiLast, N));

			}

		};
		setTridiagonalMatrixAlgorithm(tridiagonal);
	}

	private void initConst(ParticipatingMedium problem) {
		var p = (ThermoOpticalProperties)problem.getProperties();
		final double Np = (double) p.getPlanckNumber().getValue();
		final double opticalThickness = (double) p.getOpticalThickness().getValue();

		HX2 = hx * hx;
		adjustSchemeWeight();

		ONE_MINUS_SIGMA = 1.0 - sigma;
		TAU0_NP = opticalThickness / Np;

		final double Bi2HX = Bi1 * hx;
		ONE_PLUS_Bi1_HX = 1. + Bi2HX;

		_2TAUHX = 2.0 * tau * hx;
		HX2_2TAU = HX2 / (2.0 * tau);
		ONE_MINUS_SIGMA_NP = ONE_MINUS_SIGMA / Np;
		_2TAU_ONE_MINUS_SIGMA = 2.0 * tau * ONE_MINUS_SIGMA;
		BETA1_FACTOR = 1.0 / (HX2 + 2.0 * tau * sigma * ONE_PLUS_Bi1_HX);
		SIGMA_NP = sigma / Np;
		HX_NP = hx / Np;

		final double sigma_HX2 = sigma / HX2;
		var tridiagonal = getTridiagonalMatrixAlgorithm();
		tridiagonal.setCoefA(sigma_HX2);
		tridiagonal.setCoefB(1. / tau + 2. * sigma_HX2);
		tridiagonal.setCoefC(sigma_HX2);
		final double alpha0 = 1.0 / (HX2_2TAU / sigma + ONE_PLUS_Bi1_HX);
		tridiagonal.setAlpha(1, alpha0);
		tridiagonal.evaluateAlpha();
	}

	@Override
	public void solve(ParticipatingMedium problem) throws SolverException {
		this.prepare(problem);
		initConst(problem);

		setCalculationStatus(rte.compute(getPreviousSolution()));
		this.runTimeSequence(problem);
		
		var status = getCalculationStatus();
		if (status != RTECalculationStatus.NORMAL)
			throw new SolverException(status.toString());

	}

	@Override
	public double pulse(final int m) {
		var pulse = getDiscretePulse();
		return (pulse.laserPowerAt((m - 1 + EPS) * tau) * ONE_MINUS_SIGMA
				+ pulse.laserPowerAt((m - EPS) * tau) * sigma);
	}

	@Override
	public double firstBeta(final int m) {
		var U = getPreviousSolution();
		final double phi = TAU0_NP * fluxes.fluxDerivativeFront();
		return (_2TAUHX
				* (getCurrentPulseValue() - SIGMA_NP * fluxes.getFlux(0) - ONE_MINUS_SIGMA_NP * fluxes.getStoredFlux(0))
				+ HX2 * (U[0] + phi * tau) + _2TAU_ONE_MINUS_SIGMA * (U[1] - U[0] * ONE_PLUS_Bi1_HX)) * BETA1_FACTOR;
	}

	@Override
	public double evalRightBoundary(final int m, final double alphaN, final double betaN) {
		final double phi = TAU0_NP * fluxes.fluxDerivativeRear();
		final var U = getPreviousSolution();
		return (sigma * betaN + HX2_2TAU * U[N] + 0.5 * HX2 * phi
				+ ONE_MINUS_SIGMA * (U[N - 1] - U[N] * ONE_PLUS_Bi1_HX)
				+ HX_NP * (sigma * fluxes.getFlux(N) + ONE_MINUS_SIGMA * fluxes.getStoredFlux(N)))
				/ (HX2_2TAU + sigma * (ONE_PLUS_Bi1_HX - alphaN));
	}

	private void adjustSchemeWeight() {
		final double newSigma = 0.5 - HX2 / (12.0 * tau);
		setWeight(derive(SCHEME_WEIGHT, newSigma > 0 ? newSigma : 0.5));
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
		if (type == SCHEME_WEIGHT)
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