package pulse.search.math;

import pulse.properties.Messages;
import pulse.properties.NumericProperty;

public enum ObjectiveFunctionIndex implements Index {

	DIFFUSIVITY(true), MAX_TEMP(true), HEAT_LOSSES(true), BASELINE(false);
	
	private boolean active;
	
	private final static String HEAT_LOSS_DESCRIPTOR   = Messages.getString("NumericProperty.19");
	private final static String DIFFUSIVITY_DESCRIPTOR = Messages.getString("NumericProperty.15");
	private final static String MAX_TEMP_DESCRIPTOR	   = Messages.getString("NumericProperty.20");
	private final static String BASELINE_DESCRIPTOR	   = Messages.getString("NumericProperty.16");
	
	private ObjectiveFunctionIndex(boolean value) {
		this.active = value;
	}
	
	public static ObjectiveFunctionIndex valueOf(NumericProperty p) {
		
		if(p.getSimpleName().equals( DIFFUSIVITY_DESCRIPTOR) )
			return DIFFUSIVITY;
		else if(p.getSimpleName().equals(  MAX_TEMP_DESCRIPTOR ) )
			return MAX_TEMP;
		else if(p.getSimpleName().equals(  HEAT_LOSS_DESCRIPTOR) )
			return HEAT_LOSSES; 
		else if(p.getSimpleName().equals(  BASELINE_DESCRIPTOR ) )
			return BASELINE;
		
		return null;
		
	}
	
	public boolean isActiveByDefault() {
		return active;
	}
	
}
