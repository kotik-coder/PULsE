package pulse.problem.statements.penetration;

import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.REFLECTANCE;

import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public class Insulator extends AbsorptionModel {

	protected double R;

	public Insulator() {
		R = (double) def(REFLECTANCE).getValue();
	}

	@Override
	public double absorption(SpectralRange spectrum, double x) {
		double a = (double) (this.getAbsorptivity(spectrum).getValue());
		return a * (Math.exp(-a * x) - R * Math.exp(-a * (2.0 - x))) / (1.0 - R * R * Math.exp(-2.0 * a));
	}

	public NumericProperty getReflectance() {
		return derive(REFLECTANCE, R);
	}

	public void setReflectance(NumericProperty a) {
		this.R = (double) a.getValue();

	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		super.set(type, property);
		if (type == REFLECTANCE)
			R = ((Number) property.getValue()).doubleValue();
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(def(REFLECTANCE));
		return list;
	}

}