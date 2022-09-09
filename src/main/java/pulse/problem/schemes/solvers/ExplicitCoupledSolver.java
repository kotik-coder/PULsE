package pulse.problem.schemes.solvers;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.GRID_DENSITY;
import static pulse.properties.NumericPropertyKeyword.TAU_FACTOR;
import static pulse.ui.Messages.getString;

import pulse.problem.schemes.ExplicitScheme;
import pulse.problem.schemes.RadiativeTransferCoupling;
import pulse.problem.schemes.rte.Fluxes;
import pulse.problem.schemes.rte.RTECalculationStatus;
import static pulse.problem.schemes.solvers.SolverException.SolverExceptionType.RTE_SOLVER_ERROR;
import pulse.problem.statements.ParticipatingMedium;
import pulse.problem.statements.Problem;
import pulse.problem.statements.model.ThermoOpticalProperties;
import pulse.properties.NumericProperty;

public abstract class ExplicitCoupledSolver extends ExplicitScheme
        implements Solver<ParticipatingMedium> {

    private RadiativeTransferCoupling coupling;
    private RTECalculationStatus status;
    private Fluxes fluxes;

    private double hx;
    private double a;

    private int N;

    private double HX_NP;
    private double prefactor;
    private double zeta;

    private boolean autoUpdateFluxes = true; //should be false for nonlinear solvers
    
    public ExplicitCoupledSolver() {
        this(derive(GRID_DENSITY, 80), derive(TAU_FACTOR, 0.5));
    }

    public ExplicitCoupledSolver(NumericProperty N, NumericProperty timeFactor) {
        super(N, timeFactor);
        setCoupling(new RadiativeTransferCoupling());
        status = RTECalculationStatus.NORMAL;
    }

    @Override
    public void prepare(Problem problem) throws SolverException {
        super.prepare(problem);

        var grid = getGrid();

        coupling.init((ParticipatingMedium)problem, grid);
        fluxes = coupling.getRadiativeTransferEquation().getFluxes();
        setCalculationStatus(fluxes.checkArrays());

        N = (int) grid.getGridDensity().getValue();
        hx = grid.getXStep();

        var p = (ThermoOpticalProperties) problem.getProperties();
        //combined Biot
        double Bi = (double) p.getHeatLoss().getValue() + (double) p.getConvectiveLosses().getValue();

        a = 1. / (1. + Bi * hx);

        final double opticalThickness = (double) p.getOpticalThickness().getValue();
        final double Np = (double) p.getPlanckNumber().getValue();
        final double tau = getGrid().getTimeStep();

        HX_NP = hx / Np;
        prefactor = tau * opticalThickness / Np;

        zeta = (double) ((ParticipatingMedium)problem).getGeometricFactor().getValue();
    }

    @Override
    public void timeStep(int m) throws SolverException {
        explicitSolution();
    }

    @Override
    public void finaliseStep() throws SolverException {
        super.finaliseStep();
        if (autoUpdateFluxes) {
            var rte = this.getCoupling().getRadiativeTransferEquation();
            setCalculationStatus(rte.compute(getCurrentSolution()));
        }
        coupling.getRadiativeTransferEquation().getFluxes().store();
    }

    @Override
    public void solve(ParticipatingMedium problem) throws SolverException {
        prepare(problem);
        runTimeSequence(problem);
    }

    @Override
    public void explicitSolution() {
        /*
		 * Uses the heat equation explicitly to calculate the grid-function everywhere
		 * except the boundaries
         */
        super.explicitSolution();

        var V = getCurrentSolution();

        double pls = getCurrentPulseValue();

        // Front face
        V[0] = (V[1] + hx * zeta * pls - HX_NP * fluxes.getFlux(0)) * a;
        // Rear face
        V[N] = (V[N - 1] + hx * (1.0 - zeta) * pls + HX_NP * fluxes.getFlux(N)) * a;
    }

    @Override
    public boolean normalOperation() {
        return super.normalOperation() && (status == RTECalculationStatus.NORMAL);
    }

    @Override
    public double phi(final int i) {
        return prefactor * fluxes.fluxDerivative(i);
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
    public Class<? extends Problem>[] domain() {
        return new Class[]{ParticipatingMedium.class};
    }

    public void setCalculationStatus(RTECalculationStatus calculationStatus) throws SolverException {
        this.status = calculationStatus;
        if (status != RTECalculationStatus.NORMAL) {
            throw new SolverException(status.toString(), 
                    RTE_SOLVER_ERROR);
        }
    }
    
    public final boolean isAutoUpdateFluxes() {
        return this.autoUpdateFluxes;
    }
    
    public final void setAutoUpdateFluxes(boolean auto) {
        this.autoUpdateFluxes = auto;
    }

}