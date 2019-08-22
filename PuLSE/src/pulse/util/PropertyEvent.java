package pulse.util;

import pulse.properties.Property;

public class PropertyEvent {

	private Object source;
	private Property property;	
	
	public PropertyEvent(Object source, Property property) {
		this.source = source;
		this.property = property;
	}

	public Object getSource() {
		return source;
	}

	public Property getProperty() {
		return property;
	}
	
}
