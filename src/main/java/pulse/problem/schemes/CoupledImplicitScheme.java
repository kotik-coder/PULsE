package pulse.problem.schemes;

import static pulse.math.MathUtils.fastPowLoop;
import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericPropertyKeyword.NONLINEAR_PRECISION;

import java.util.List;

import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.statements.ParticipatingMedium;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public abstract class CoupledImplicitScheme extends ImplicitScheme {
	
	private RadiativeTransferCoupling coupling;
	private RTECalculationStatus calculationStatus;
	private double nonlinearPrecision;
	
	private int N;
	private double pls;

	public CoupledImplicitScheme(NumericProperty N, NumericProperty timeFactor) {
		super();
		setGrid(new Grid(N, timeFactor));
		this.N = (int) N.getValue();
		nonlinearPrecision = (double) def(NONLINEAR_PRECISION).getValue();
		setCoupling(new RadiativeTransferCoupling());
		calculationStatus = RTECalculationStatus.NORMAL;
	}

	public CoupledImplicitScheme(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		this(N, timeFactor);
		setTimeLimit(timeLimit);
	}
	
	@Override
	public void timeStep(final int m) {
		pls = pulse(m);
		final double errorSq = fastPowLoop((double) getNonlinearPrecision().getValue(), 2);
		
		var V = getCurrentSolution();
		
		for (double V_0 = errorSq + 1, V_N = errorSq + 1; (fastPowLoop((V[0] - V_0), 2) > errorSq)
				|| (fastPowLoop((V[N] - V_N), 2) > errorSq); setCalculationStatus( coupling.getRadiativeTransferEquation().compute(V) ) ) {

			V_N = V[N];
			V_0 = V[0];
			super.timeStep(m);

		}	
		
	}
	
	public RadiativeTransferCoupling getCoupling() {
		return coupling;
	}

	public void setCoupling(RadiativeTransferCoupling coupling) {
		this.coupling = coupling;
		this.coupling.setParent(this);
	}
	
	@Override
	public void finaliseStep() {
		super.finaliseStep();
		coupling.getRadiativeTransferEquation().getFluxes().store();
	}
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(def(NONLINEAR_PRECISION));
		return list;
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
		} else
			super.set(type, property);
	}

	public RTECalculationStatus getCalculationStatus() {
		return calculationStatus;
	}

	public void setCalculationStatus(RTECalculationStatus calculationStatus) {
		this.calculationStatus = calculationStatus;
	}

	public double getCurrentPulseValue() {
		return pls;
	}
	
}