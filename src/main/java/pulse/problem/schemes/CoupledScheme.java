package pulse.problem.schemes;

import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericPropertyKeyword.NONLINEAR_PRECISION;

import java.util.List;

import pulse.problem.statements.ParticipatingMedium;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public abstract class CoupledScheme extends DifferenceScheme {
	
	private RadiativeTransferCoupling coupling;
	private double nonlinearPrecision;

	public CoupledScheme(NumericProperty N, NumericProperty timeFactor) {
		super();
		nonlinearPrecision = (double) NumericProperty.def(NONLINEAR_PRECISION).getValue();
		setGrid(new Grid(N, timeFactor));
		setCoupling(new RadiativeTransferCoupling());
	}

	public CoupledScheme(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		this(N, timeFactor);
		setTimeLimit(timeLimit);
	}
	
	public RadiativeTransferCoupling getCoupling() {
		return coupling;
	}

	public void setCoupling(RadiativeTransferCoupling coupling) {
		this.coupling = coupling;
		this.coupling.setParent(this);
	}
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(def(NONLINEAR_PRECISION));
		return list;
	}
	
	public NumericProperty getNonlinearPrecision() {
		return NumericProperty.derive(NONLINEAR_PRECISION, nonlinearPrecision);
	}

	public void setNonlinearPrecision(NumericProperty nonlinearPrecision) {
		this.nonlinearPrecision = (double) nonlinearPrecision.getValue();
	}

	@Override
	public Class<? extends Problem> domain() {
		return ParticipatingMedium.class;
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		if (type == NONLINEAR_PRECISION) {
			setNonlinearPrecision(property);
		} else
			super.set(type, property);
	}
	
}