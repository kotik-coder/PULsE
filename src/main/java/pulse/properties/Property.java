package pulse.properties;

/**
 * The basic interface for properties. The only declared functionality
 * consists in the ability to report the associated value and deliver 
 * text description.
 */

public interface Property {

	/**
	 * Retrieves the value of this {@code Property}.
	 * @return an object representing the value of this {@code Property}
	 */
	
	public Object getValue();
	
	/**
	 * Formats the value so that it is suitable for output using the GUI or console.
	 * @return a formatted {@code String} representing the {@code value}
	 */
	
	public default String formattedValue() {
		return getValue().toString();
	};
	
	/**
	 * Creates a {@code String} to describe this property (often used in GUI applications).
	 * @param addHtmlTags if {@code true}, adds the 'html' tags at both ends of the descriptor {@code String}.
	 * @return a {@code String}, with or without 'html' tags, describing this {@code Property}
	 */
	
	public String getDescriptor(boolean addHtmlTags);

}