package pulse.problem.statements;

import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public class Insulator extends AbsorptionModel {

	protected double R;
	
	public Insulator() {
		R = (double)NumericProperty.def(NumericPropertyKeyword.REFLECTANCE).getValue();
	}

	@Override
	public double absorption(SpectralRange spectrum, double x) {
		double a = (double) (this.getAbsorptivity(spectrum).getValue());
		return a*( Math.exp(-a*x) - R*Math.exp(-a*(2.0 - x)) ) / (1.0 - R*R*Math.exp(-2.0*a));
	}
	
	public NumericProperty getReflectance() {
		return NumericProperty.derive(NumericPropertyKeyword.REFLECTANCE, R);
	}
	
	public void setReflectance(NumericProperty a) {
		this.R = (double)a.getValue();
		
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		super.set(type, property);
		
		NumericPropertyKeyword prop = type;
		double newVal = ((Number)property.getValue()).doubleValue();
		
		switch(prop) {
			case REFLECTANCE		: 	R = newVal; 			return;
		default:
			break;
		}
	}
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(NumericProperty.def(NumericPropertyKeyword.REFLECTANCE));
		return list;
	}
	
}