package pulse.problem.statements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

public abstract class AbsorptionModel extends PropertyHolder implements Reflexive {
	
	private Map<SpectralRange,NumericProperty> absorptionMap; 
	
	protected AbsorptionModel() {
		setPrefix("Absorption model");
		absorptionMap = new HashMap<SpectralRange,NumericProperty>();
		absorptionMap.put(
				SpectralRange.LASER, 
				NumericProperty.theDefault(NumericPropertyKeyword.
								LASER_ABSORPTIVITY) );
		absorptionMap.put(
				SpectralRange.THERMAL, 
				NumericProperty.theDefault(NumericPropertyKeyword.
								THERMAL_ABSORPTIVITY) );
	}

	public abstract double absorption(SpectralRange range, double x);
	
	public NumericProperty getLaserAbsorptivity() {
		return absorptionMap.get(SpectralRange.LASER);
	}
	
	public NumericProperty getThermalAbsorptivity() {
		return absorptionMap.get(SpectralRange.THERMAL);
	}
	
	public NumericProperty getAbsorptivity(SpectralRange spectrum) {
		return absorptionMap.get(spectrum);
	}
	
	public void setAbsorptivity(SpectralRange range, NumericProperty a) {
		absorptionMap.put(range, a);		
	}
	
	public void setLaserAbsorptivity(NumericProperty a) {
		absorptionMap.put(SpectralRange.LASER, a);		
	}
	
	public void setThermalAbsorptivity(NumericProperty a) {
		absorptionMap.put(SpectralRange.THERMAL, a);		
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		
		switch(type) {
			case LASER_ABSORPTIVITY		: 	
				absorptionMap.put(SpectralRange.LASER, property);  			
				return;
			case THERMAL_ABSORPTIVITY	:
				absorptionMap.put(SpectralRange.THERMAL, property);  			
				return;
		}
		
	}
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<Property>();
		list.add(
				NumericProperty.def(
						NumericPropertyKeyword.LASER_ABSORPTIVITY
									)
				);
		list.add(
				NumericProperty.def(
						NumericPropertyKeyword.THERMAL_ABSORPTIVITY
									)
				);		
		return list;				
	}
	
	public enum SpectralRange {
		LASER("Laser Absorption"), THERMAL("Thermal Radiation Absorption");
		
		String name;
		
		SpectralRange(String name) {
			this.name = name;
		}
		
		@Override
		public String toString() {
			return name;
		}
		
		public NumericPropertyKeyword typeOfAbsorption() {
			return this == SpectralRange.LASER ? 
					NumericPropertyKeyword.LASER_ABSORPTIVITY :
					NumericPropertyKeyword.THERMAL_ABSORPTIVITY;
		}
		
	}
	
}