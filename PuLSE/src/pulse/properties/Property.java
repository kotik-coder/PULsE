package pulse.properties;

public interface Property {

	public Object getValue();
	public default String formattedValue() {
		return getValue().toString();
	};
	public String getDescriptor(boolean addHtmlTags);

}
