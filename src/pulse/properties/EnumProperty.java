package pulse.properties;

/**
 * A constant-type {@code Property} that can be evaluated from a {@code String}.
 *
 */

public interface EnumProperty extends Property {

	/**
	 * Uses the {@code string} to create an {@code EnumProperty}
	 * @param string the string, which presumably contains information sufficient to build an {@code EnumProperty}
	 * @return the respective {@code EnumProperty}
	 */
	
	public EnumProperty evaluate(String string);
	
}