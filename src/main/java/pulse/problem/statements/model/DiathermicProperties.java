package pulse.problem.statements.model;

import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.DIATHERMIC_COEFFICIENT;

import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public class DiathermicProperties extends ThermalProperties {

	private double diathermicCoefficient;

	public DiathermicProperties() {
		super();
		this.diathermicCoefficient = (double) def(DIATHERMIC_COEFFICIENT).getValue();
	}

	public DiathermicProperties(ThermalProperties p) {
		super(p);
		var property = p instanceof DiathermicProperties
				? ((DiathermicProperties) p).getDiathermicCoefficient()
				: def(DIATHERMIC_COEFFICIENT);
		this.diathermicCoefficient = (double)property.getValue();
	}

	public ThermalProperties copy() {
		return new ThermalProperties(this);
	}

	public NumericProperty getDiathermicCoefficient() {
		return derive(DIATHERMIC_COEFFICIENT, diathermicCoefficient);
	}

	public void setDiathermicCoefficient(NumericProperty diathermicCoefficient) {
		requireType(diathermicCoefficient, DIATHERMIC_COEFFICIENT);
		this.diathermicCoefficient = (double) diathermicCoefficient.getValue();
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		if (type == DIATHERMIC_COEFFICIENT) {
			diathermicCoefficient = ((Number) property.getValue()).doubleValue();
		} else {
			super.set(type, property);
		}
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(def(DIATHERMIC_COEFFICIENT));
		return list;
	}

}