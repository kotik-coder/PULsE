package pulse.properties;

import pulse.search.math.Vector;

public class VectorProperty implements Property {

	String name;
	Vector value;
	
	public VectorProperty(String name, Vector value) {
		this.name = name;
		this.value = value;
	}
	
	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public String getSimpleName() {
		return name;
	}
	
}
