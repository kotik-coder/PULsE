package pulse.problem.statements.penetration;

import static pulse.problem.statements.penetration.SpectralRange.LASER;
import static pulse.problem.statements.penetration.SpectralRange.THERMAL;
import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericProperty.theDefault;
import static pulse.properties.NumericPropertyKeyword.LASER_ABSORPTIVITY;
import static pulse.properties.NumericPropertyKeyword.THERMAL_ABSORPTIVITY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

public abstract class AbsorptionModel extends PropertyHolder implements Reflexive {

	private Map<SpectralRange, NumericProperty> absorptionMap;

	protected AbsorptionModel() {
		setPrefix("Absorption model");
		absorptionMap = new HashMap<>();
		absorptionMap.put(LASER, theDefault(LASER_ABSORPTIVITY));
		absorptionMap.put(THERMAL, theDefault(THERMAL_ABSORPTIVITY));
	}

	public abstract double absorption(SpectralRange range, double x);

	public NumericProperty getLaserAbsorptivity() {
		return absorptionMap.get(LASER);
	}

	public NumericProperty getThermalAbsorptivity() {
		return absorptionMap.get(THERMAL);
	}

	public NumericProperty getAbsorptivity(SpectralRange spectrum) {
		return absorptionMap.get(spectrum);
	}

	public void setAbsorptivity(SpectralRange range, NumericProperty a) {
		absorptionMap.put(range, a);
	}

	public void setLaserAbsorptivity(NumericProperty a) {
		absorptionMap.put(LASER, a);
	}

	public void setThermalAbsorptivity(NumericProperty a) {
		absorptionMap.put(THERMAL, a);
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {

		switch (type) {
		case LASER_ABSORPTIVITY:
			absorptionMap.put(LASER, property);
			break;
		case THERMAL_ABSORPTIVITY:
			absorptionMap.put(THERMAL, property);
			break;
		default:
			break;
		}

	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " : " + absorptionMap.get(LASER) + " ; " + absorptionMap.get(THERMAL);
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(def(LASER_ABSORPTIVITY));
		list.add(def(THERMAL_ABSORPTIVITY));
		return list;
	}

}