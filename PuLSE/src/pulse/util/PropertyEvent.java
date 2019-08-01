package pulse.util;

import pulse.properties.Property;

public class PropertyEvent {

	private PropertyHolder source;
	private Property property;
	
	public PropertyEvent(PropertyHolder source, Property property) {
		this.source = source;
		this.property = property;
	}

	public PropertyHolder getSource() {
		return source;
	}

	public Property getProperty() {
		return property;
	}
	
}
