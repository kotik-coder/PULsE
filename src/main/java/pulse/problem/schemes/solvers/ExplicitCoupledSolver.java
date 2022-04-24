package pulse.problem.schemes.solvers;

import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.GRID_DENSITY;
import static pulse.properties.NumericPropertyKeyword.NONLINEAR_PRECISION;
import static pulse.properties.NumericPropertyKeyword.TAU_FACTOR;
import static pulse.ui.Messages.getString;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ExplicitScheme;
import pulse.problem.schemes.FixedPointIterations;
import pulse.problem.schemes.RadiativeTransferCoupling;
import pulse.problem.schemes.rte.Fluxes;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.schemes.rte.RadiativeTransferSolver;
import pulse.problem.statements.ParticipatingMedium;
import pulse.problem.statements.Problem;
import pulse.problem.statements.model.ThermoOpticalProperties;
import pulse.properties.NumericProperty;

public class ExplicitCoupledSolver extends ExplicitScheme implements Solver<ParticipatingMedium>, FixedPointIterations {

    private RadiativeTransferCoupling coupling;
    private RadiativeTransferSolver rte;
    private RTECalculationStatus status;
    private Fluxes fluxes;

    private double hx;
    private double a;
    private double nonlinearPrecision;
    private double pls;

    private int N;

    private double HX_NP;
    private double prefactor;

    public ExplicitCoupledSolver() {
        this(derive(GRID_DENSITY, 80), derive(TAU_FACTOR, 0.5));
    }

    public ExplicitCoupledSolver(NumericProperty N, NumericProperty timeFactor) {
        super(N, timeFactor);
        nonlinearPrecision = (double) def(NONLINEAR_PRECISION).getValue();
        setCoupling(new RadiativeTransferCoupling());
        status = RTECalculationStatus.NORMAL;
    }

    private void prepare(ParticipatingMedium problem) throws SolverException {
        super.prepare(problem);

        var grid = getGrid();

        coupling.init(problem, grid);
        rte = coupling.getRadiativeTransferEquation();
        fluxes = coupling.getRadiativeTransferEquation().getFluxes();
        setCalculationStatus(fluxes.checkArrays());
                
        N = (int) grid.getGridDensity().getValue();
        hx = grid.getXStep();

        var p = (ThermoOpticalProperties) problem.getProperties();
        double Bi = (double) p.getHeatLoss().getValue();

        a = 1. / (1. + Bi * hx);

        final double opticalThickness = (double) p.getOpticalThickness().getValue();
        final double Np = (double) p.getPlanckNumber().getValue();
        final double tau = getGrid().getTimeStep();

        HX_NP = hx / Np;
        prefactor = tau * opticalThickness / Np;
    }

    @Override
    public void solve(ParticipatingMedium problem) throws SolverException {
        this.prepare(problem);        
        setCalculationStatus(coupling.getRadiativeTransferEquation().compute(getPreviousSolution()));
        runTimeSequence(problem);
    }

    @Override
    public boolean normalOperation() {
        return super.normalOperation() && (status == RTECalculationStatus.NORMAL);
    }

    @Override
    public void timeStep(int m) throws SolverException {
        pls = pulse(m);
        doIterations(getCurrentSolution(), nonlinearPrecision, m);
    }

    @Override
    public void iteration(final int m) {
        /*
		 * Uses the heat equation explicitly to calculate the grid-function everywhere
		 * except the boundaries
         */
        explicitSolution();

        var V = getCurrentSolution();

        // Front face
        V[0] = (V[1] + hx * pls - HX_NP * fluxes.getFlux(0)) * a;
        // Rear face
        V[N] = (V[N - 1] + HX_NP * fluxes.getFlux(N)) * a;
    }

    @Override
    public void finaliseIteration(double[] V) throws SolverException {
        FixedPointIterations.super.finaliseIteration(V);
        setCalculationStatus(rte.compute(V));
    }

    @Override
    public double phi(final int i) {
        return prefactor * fluxes.fluxDerivative(i);
    }

    @Override
    public void finaliseStep() throws SolverException {
        super.finaliseStep();
        coupling.getRadiativeTransferEquation().getFluxes().store();
    }

    public RadiativeTransferCoupling getCoupling() {
        return coupling;
    }

    public final void setCoupling(RadiativeTransferCoupling coupling) {
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
    
    public void setCalculationStatus(RTECalculationStatus calculationStatus) throws SolverException {
        this.status = calculationStatus;
        if(status != RTECalculationStatus.NORMAL)
            throw new SolverException(status.toString());
    }

}
