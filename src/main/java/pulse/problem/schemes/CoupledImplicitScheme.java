package pulse.problem.schemes;

import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.NONLINEAR_PRECISION;

import java.util.Set;

import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.schemes.solvers.SolverException;
import pulse.problem.statements.ParticipatingMedium;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;

public abstract class CoupledImplicitScheme extends ImplicitScheme implements FixedPointIterations {

    private RadiativeTransferCoupling coupling;
    private RTECalculationStatus calculationStatus;
    private double nonlinearPrecision;

    private double pls;

    public CoupledImplicitScheme(NumericProperty N, NumericProperty timeFactor) {
        super();
        setGrid(new Grid(N, timeFactor));
        nonlinearPrecision = (double) def(NONLINEAR_PRECISION).getValue();
        setCoupling(new RadiativeTransferCoupling());
        calculationStatus = RTECalculationStatus.NORMAL;
    }

    public CoupledImplicitScheme(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
        this(N, timeFactor);
        setTimeLimit(timeLimit);
    }

    @Override
    public void timeStep(final int m) throws SolverException {
        pls = pulse(m);
        doIterations(getCurrentSolution(), nonlinearPrecision, m);
    }

    @Override
    public void iteration(final int m) throws SolverException {
        super.timeStep(m);
    }

    @Override
    public void finaliseIteration(double[] V) throws SolverException {
        FixedPointIterations.super.finaliseIteration(V);
        var rte = coupling.getRadiativeTransferEquation();
        setCalculationStatus(coupling.getRadiativeTransferEquation().compute(V));
    }

    public RadiativeTransferCoupling getCoupling() {
        return coupling;
    }

    public final void setCoupling(RadiativeTransferCoupling coupling) {
        this.coupling = coupling;
        this.coupling.setParent(this);
    }

    @Override
    public void finaliseStep() throws SolverException {
        super.finaliseStep();
        coupling.getRadiativeTransferEquation().getFluxes().store();
    }

    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(NONLINEAR_PRECISION);
        return set;
    }

    public NumericProperty getNonlinearPrecision() {
        return derive(NONLINEAR_PRECISION, nonlinearPrecision);
    }

    public void setNonlinearPrecision(NumericProperty nonlinearPrecision) {
        this.nonlinearPrecision = (double) nonlinearPrecision.getValue();
    }

    @Override
    public Class<? extends Problem> domain() {
        return ParticipatingMedium.class;
    }

    @Override
    public boolean normalOperation() {
        return super.normalOperation() && (getCalculationStatus() == RTECalculationStatus.NORMAL);
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        if (type == NONLINEAR_PRECISION) {
            setNonlinearPrecision(property);
        } else {
            super.set(type, property);
        }
    }

    public RTECalculationStatus getCalculationStatus() {
        return calculationStatus;
    }

    public void setCalculationStatus(RTECalculationStatus calculationStatus) throws SolverException {
        this.calculationStatus = calculationStatus;
        if(calculationStatus != RTECalculationStatus.NORMAL)
            throw new SolverException(calculationStatus.toString());
    }

    public double getCurrentPulseValue() {
        return pls;
    }

}
