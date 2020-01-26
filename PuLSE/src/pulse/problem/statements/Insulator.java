package pulse.problem.statements;

import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public class Insulator extends AbsorptionModel {

	private double R;
	
	public Insulator(SpectralRange spectrum) {
		super(spectrum);
		R = (double)NumericProperty.def(NumericPropertyKeyword.REFLECTANCE).getValue();
	}

	@Override
	public double absorption(double x) {
		return a0*( Math.exp(-a0*x) - R*Math.exp(-a0*(2.0 - x)) ) / (1.0 - R*R*Math.exp(-2.0*a0));
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
		
		NumericPropertyKeyword prop = (NumericPropertyKeyword)type;
		double newVal = ((Number)property.getValue()).doubleValue();
		
		switch(prop) {
			case REFLECTANCE		: 	R = newVal; 			return;
		}
	}
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(NumericProperty.def(NumericPropertyKeyword.REFLECTANCE));
		return list;
	}
	
}