package pulse.problem.statements;

import java.util.ArrayList;
import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

public abstract class AbsorptionModel extends PropertyHolder implements Reflexive {
	
	protected double a0;	
	private SpectralRange spectrum;
	
	protected AbsorptionModel(SpectralRange spectrum) {
		a0 = (double)NumericProperty.def(NumericPropertyKeyword.ABSORPTIVITY).getValue();
		this.spectrum = spectrum;
	}
	
	public SpectralRange getSpectralRange() {
		return spectrum;
	}
	
	public abstract double absorption(double x);
	
	public NumericProperty getAbsorptivity() {
		return NumericProperty.derive(NumericPropertyKeyword.ABSORPTIVITY, a0);
	}
	
	public void setAbsorptivity(NumericProperty a) {
		this.a0 = (double)a.getValue();
		
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		NumericPropertyKeyword prop = (NumericPropertyKeyword)type;
		double newVal = ((Number)property.getValue()).doubleValue();
		
		switch(prop) {
			case ABSORPTIVITY		: 	a0 = newVal; 			return;
		}
	}
	
	public  String getDescriptor() {
		return spectrum.toString();
	}
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<Property>();
		list.add(NumericProperty.def(NumericPropertyKeyword.ABSORPTIVITY));
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
		
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " for " + spectrum;
	}
	
}