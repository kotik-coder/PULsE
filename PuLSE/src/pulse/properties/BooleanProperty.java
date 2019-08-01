package pulse.properties;

public class BooleanProperty implements Property {

	private boolean value;
	private String simpleName;
	
	public BooleanProperty(String name, boolean value) {
		this.value = value;
		this.simpleName = name;
	}

	public BooleanProperty(BooleanProperty booleanProperty) {
		this(booleanProperty.simpleName, booleanProperty.value);
	}

	public Object getValue() {
		return value;
	}

	public void setValue(boolean value) {
		this.value = value;
	}

	public String getSimpleName() {
		return simpleName;
	}

	public void setName(String name) {
		this.simpleName = name;
	}	
	
	public String toString() {
		return Boolean.toString(value);
	}
	
}
