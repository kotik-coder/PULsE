package pulse.problem.statements;

import java.util.List;

import pulse.math.IndexedVector;
import pulse.problem.schemes.rte.MathUtils;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.ui.Messages;

public class DiathermicMedium extends LinearisedProblem {

	private double diathermicCoefficient;
	private final static boolean DEBUG = false;

	public DiathermicMedium() {
		super();
		this.diathermicCoefficient = (double) NumericProperty.def(NumericPropertyKeyword.DIATHERMIC_COEFFICIENT)
				.getValue();
	}

	public DiathermicMedium(NumericProperty diathermicCoefficient) {
		super();
		this.diathermicCoefficient = (double) (diathermicCoefficient.getValue());
	}

	public DiathermicMedium(Problem sdd) {
		super(sdd);
		if (sdd instanceof DiathermicMedium)
			this.diathermicCoefficient = ((DiathermicMedium) sdd).diathermicCoefficient;
		else
			this.diathermicCoefficient = (double) NumericProperty.def(NumericPropertyKeyword.DIATHERMIC_COEFFICIENT)
					.getValue();
	}

	public DiathermicMedium(DiathermicMedium sdd) {
		super(sdd);
		this.diathermicCoefficient = sdd.diathermicCoefficient;
	}

	public NumericProperty getDiathermicCoefficient() {
		return NumericProperty.derive(NumericPropertyKeyword.DIATHERMIC_COEFFICIENT, diathermicCoefficient);
	}

	public void setDiathermicCoefficient(NumericProperty diathermicCoefficient) {
		this.diathermicCoefficient = (double) diathermicCoefficient.getValue();
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		NumericPropertyKeyword prop = type;
		double newVal = ((Number) property.getValue()).doubleValue();

		switch (prop) {
		case DIATHERMIC_COEFFICIENT:
			diathermicCoefficient = newVal;
			break;
		default:
			super.set(type, property);
			break;
		}

	}

	@Override
	public boolean isEnabled() {
		return !DEBUG;
	}

	@Override
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		super.optimisationVector(output, flags);

		for (int i = 0, size = output[0].dimension(); i < size; i++) {
			switch (output[0].getIndex(i)) {
			case DIATHERMIC_COEFFICIENT:
				output[0].set(i, MathUtils.atanh(2.0 * diathermicCoefficient - 1.0) );
				output[1].set(i, 10.0);
				break;
			default:
				continue;
			}
		}

	}

	@Override
	public void assign(IndexedVector params) {
		super.assign(params);

		for (int i = 0, size = params.dimension(); i < size; i++) {
			switch (params.getIndex(i)) {
			case DIATHERMIC_COEFFICIENT:
				diathermicCoefficient = 0.5 * (Math.tanh(params.get(i)) + 1.0);
				break;
			case HEAT_LOSS:
				if( areThermalPropertiesLoaded() ) {
					double emissivity = emissivity();
					if(emissivity > 1.0)
						System.out.println(emissivity);
					diathermicCoefficient = emissivity/(2.0 - emissivity);
				}
				break;
			default:
				continue;
			}
		}
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(NumericProperty.def(NumericPropertyKeyword.DIATHERMIC_COEFFICIENT));
		return list;
	}

	@Override
	public String toString() {
		return Messages.getString("DiathermicProblem.Descriptor");
	}

}