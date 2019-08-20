package pulse.util;

import pulse.properties.Property;

public class PropertyEvent {

	private PropertyHolder source;
	private Property property;
	private Object guiComponent;
	
	public PropertyEvent(Object guiComponent, PropertyHolder source, Property property) {
		this.source = source;
		this.property = property;
		this.guiComponent = guiComponent;
	}

	public PropertyHolder getSource() {
		return source;
	}
	
	public Object getSourceComponent() {
		return guiComponent;
	}

	public Property getProperty() {
		return property;
	}
	
}
