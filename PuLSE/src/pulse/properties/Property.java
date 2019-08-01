package pulse.properties;

public interface Property {

	public Object getValue();
	public String getSimpleName();
	public default String formattedValue() {
		return getValue().toString();
	};
	
}
