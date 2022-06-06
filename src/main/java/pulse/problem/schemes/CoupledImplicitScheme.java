package pulse.problem.schemes;

import static pulse.properties.NumericPropertyKeyword.NONLINEAR_PRECISION;

import java.util.Set;

import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.schemes.solvers.SolverException;
import pulse.problem.statements.ParticipatingMedium;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;

public abstract class CoupledImplicitScheme extends ImplicitScheme {

    private RadiativeTransferCoupling coupling;
    private RTECalculationStatus calculationStatus;
    private boolean autoUpdateFluxes = true; //should be false for nonlinear solvers

    public CoupledImplicitScheme(NumericProperty N, NumericProperty timeFactor) {
        super();
        setGrid(new Grid(N, timeFactor));
        setCoupling(new RadiativeTransferCoupling());
        calculationStatus = RTECalculationStatus.NORMAL;
    }

    public CoupledImplicitScheme(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
        this(N, timeFactor);
        setTimeLimit(timeLimit);
    }
    
    @Override
    public void finaliseStep() throws SolverException {
        super.finaliseStep();
        if(autoUpdateFluxes) {
            var rte = this.getCoupling().getRadiativeTransferEquation();
            setCalculationStatus(rte.compute(getCurrentSolution()));
        }
        coupling.getRadiativeTransferEquation().getFluxes().store();
    }

    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(NONLINEAR_PRECISION);
        return set;
    }

    @Override
    public boolean normalOperation() {
        return super.normalOperation() && (getCalculationStatus() == RTECalculationStatus.NORMAL);
    }

    public final RTECalculationStatus getCalculationStatus() {
        return calculationStatus;
    }

    public final void setCalculationStatus(RTECalculationStatus calculationStatus) throws SolverException {
        this.calculationStatus = calculationStatus;
        if (calculationStatus != RTECalculationStatus.NORMAL) {
            throw new SolverException(calculationStatus.toString());
        }
    }
    
    public final RadiativeTransferCoupling getCoupling() {
        return coupling;
    }

    public final void setCoupling(RadiativeTransferCoupling coupling) {
        this.coupling = coupling;
        this.coupling.setParent(this);
    }
    
    public final boolean isAutoUpdateFluxes() {
        return this.autoUpdateFluxes;
    }
    
    public final void setAutoUpdateFluxes(boolean auto) {
        this.autoUpdateFluxes = auto;
    }
    
    @Override
    public Class<? extends Problem>[] domain() {
        return new Class[]{ParticipatingMedium.class};
    }

}