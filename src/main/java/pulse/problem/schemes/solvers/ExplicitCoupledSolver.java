package pulse.problem.schemes.solvers;

import static pulse.math.MathUtils.fastPowLoop;
import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericPropertyKeyword.GRID_DENSITY;
import static pulse.properties.NumericPropertyKeyword.NONLINEAR_PRECISION;
import static pulse.properties.NumericPropertyKeyword.TAU_FACTOR;
import static pulse.ui.Messages.getString;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ExplicitScheme;
import pulse.problem.schemes.RadiativeTransferCoupling;
import pulse.problem.schemes.rte.Fluxes;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.statements.ParticipatingMedium;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;

public class ExplicitCoupledSolver extends ExplicitScheme implements Solver<ParticipatingMedium> {

	private RadiativeTransferCoupling coupling;
	private RTECalculationStatus status;
	private Fluxes fluxes;
	
	private double hx;
	private double a;
	private double nonlinearPrecision;
	
	private int N;
	
	private double HX_NP;
	private double prefactor;
	
	private double errorSq;

	public ExplicitCoupledSolver() {
		this( derive(GRID_DENSITY, 80), derive(TAU_FACTOR, 0.5) );
	}

	public ExplicitCoupledSolver(NumericProperty N, NumericProperty timeFactor) {
		super( N, timeFactor );
		nonlinearPrecision = (double) def(NONLINEAR_PRECISION).getValue();
		setCoupling(new RadiativeTransferCoupling());
		status = RTECalculationStatus.NORMAL;
	}

	private void prepare(ParticipatingMedium problem) {
		super.prepare(problem);

		var grid = getGrid();

		coupling.init(problem, grid);
		fluxes = coupling.getRadiativeTransferEquation().getFluxes();

		N = (int) grid.getGridDensity().getValue();
		hx = grid.getXStep();

		double Bi = (double) problem.getHeatLoss().getValue();

		a = 1. / (1. + Bi * hx);

		final double opticalThickness = (double) problem.getOpticalThickness().getValue();
		final double Np = (double) problem.getPlanckNumber().getValue();
		final double tau = getGrid().getTimeStep();
		
		HX_NP = hx / Np;
		prefactor = tau * opticalThickness / Np;

		errorSq = nonlinearPrecision*nonlinearPrecision;
	}

	@Override
	public void solve(ParticipatingMedium problem) throws SolverException {
		this.prepare(problem);
		status = coupling.getRadiativeTransferEquation().compute(getPreviousSolution());
		runTimeSequence(problem);

		if (status != RTECalculationStatus.NORMAL)
			throw new SolverException(status.toString());
	}
	
	@Override
	public boolean normalOperation() {
		return super.normalOperation() && (status == RTECalculationStatus.NORMAL);
	}

	@Override
	public void timeStep(int m) {
		double pls = pulse(m);
		
		var rte = coupling.getRadiativeTransferEquation();
		
		var V = getCurrentSolution();
		
		for (double V_0 = Double.POSITIVE_INFINITY, V_N = Double.POSITIVE_INFINITY; (fastPowLoop((V[0] - V_0),
				2) > errorSq) || (fastPowLoop((V[N] - V_N), 2) > errorSq); status = rte.compute(V)) {

			/*
			 * Uses the heat equation explicitly to calculate the grid-function everywhere
			 * except the boundaries
			 */
			explicitSolution();

			// Front face
			V_0 = V[0];
			V[0] = (V[1] + hx * pls - HX_NP * fluxes.getFlux(0)) * a;
			// Rear face
			V_N = V[N];
			V[N] = (V[N - 1] + HX_NP * fluxes.getFlux(N)) * a;

		}
	}
	
	@Override
	public double phi(final int i) {
		return prefactor * fluxes.fluxDerivative(i);
	}
	
	@Override
	public void finaliseStep() {
		super.finaliseStep();
		coupling.getRadiativeTransferEquation().getFluxes().store();
	}
	
	public RadiativeTransferCoupling getCoupling() {
		return coupling;
	}

	public void setCoupling(RadiativeTransferCoupling coupling) {
		this.coupling = coupling;
		this.coupling.setParent(this);
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
		return new ExplicitCoupledSolver(grid.getGridDensity(), grid.getTimeFactor());
	}

	@Override
	public Class<? extends Problem> domain() {
		return ParticipatingMedium.class;
	}
	
}