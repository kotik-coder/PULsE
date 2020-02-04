package pulse.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static pulse.properties.NumericPropertyKeyword.*;

/**
 * A {@code Flag} is a {@code Property} that has a {@code type} represented by 
 * a {@code NumericPropertyKeyword}, a {@code boolean value}, and a {@code String}
 * with a short abbreviation describing the type of this flag (this is usually defined by the corresponding 
 * {@code NumericProperty}).
 */

public class Flag implements Property {

	private NumericPropertyKeyword index;
	private boolean value;
	private String descriptor;
	
	/**
	 * Creates a {@code Flag} with the type {@code type}. The default {@code value} is set to {@code false}. 
	 * @param type the {@code NumericPropertyKeyword} associated with this {@code Flag} 
	 */
	
	public Flag(NumericPropertyKeyword type) {
		this.index = type;
		value = false;
	}
	
	/**
	 * Creates a {@code Flag} with the following pre-specified parameters: type {@code type}, short description {@code abbreviations},
	 * and {@code value}. 
	 * @param type the {@code NumericPropertyKeyword} associated with this {@code Flag} 
	 * @param abbreviation an abbreviation (short description, usually symbolic only) associated with this {@code type}
	 * @param value the {@code boolean} value of this {@code flag}
	 */
	
	public Flag(NumericPropertyKeyword type, String abbreviation, boolean value) {
		this.index = type;
		this.descriptor = abbreviation;
		this.value = value;
	}
	
	/**
	 * A static method for converting enabled flags to a {@code List} of {@code NumericPropertyKeyword}s. 
	 * Each keyword in this list corresponds to an enabled flag in the {@code flags} {@code List}.
	 * @param flags the list of flags that needs to be analysed
	 * @return a list of {@code NumericPropertyKeyword}s corresponding to enabled {@code flag}s.
	 */

	public static List<NumericPropertyKeyword> convert(List<Flag> flags) {
		List<Flag> filtered = flags.stream().filter(flag -> (boolean) flag.getValue() ).collect(Collectors.toList());
		return filtered.stream().map(flag -> flag.getType()).collect(Collectors.toList());
	}
	
	/**
	 * The default list of {@code Flag}s used in finding the reverse solution of the heat conduction problem contains:
	 * <code>DIFFUSIVITY (true), HEAT_LOSS (true), MAXTEMP (true), BASELINE_INTERCEPT (false), BASELINE_SLOPE (false) </code>.
	 * @return a {@code List} of default {@code Flag}s
	 */
	
	public static List<Flag> defaultList() {
		List<Flag> flags = new ArrayList<Flag>();
		flags.add(new Flag(NumericPropertyKeyword.DIFFUSIVITY, NumericProperty.def(DIFFUSIVITY).getDescriptor(true), true));
		flags.add(new Flag(NumericPropertyKeyword.HEAT_LOSS, NumericProperty.def(HEAT_LOSS).getDescriptor(true), true));
		flags.add(new Flag(NumericPropertyKeyword.MAXTEMP, NumericProperty.def(MAXTEMP).getDescriptor(true), true));
		flags.add(new Flag(NumericPropertyKeyword.BASELINE_INTERCEPT, NumericProperty.def(BASELINE_INTERCEPT).getDescriptor(true), false));
		flags.add(new Flag(NumericPropertyKeyword.BASELINE_SLOPE, NumericProperty.def(BASELINE_SLOPE).getDescriptor(true), false));
		flags.add(new Flag(NumericPropertyKeyword.FOV_OUTER, NumericProperty.def(FOV_OUTER).getDescriptor(true), false));
		flags.add(new Flag(NumericPropertyKeyword.FOV_INNER, NumericProperty.def(FOV_INNER).getDescriptor(true), false));
		flags.add(new Flag(NumericPropertyKeyword.SPOT_DIAMETER, NumericProperty.def(SPOT_DIAMETER).getDescriptor(true), false));
		flags.add(new Flag(NumericPropertyKeyword.DIATHERMIC_COEFFICIENT, NumericProperty.def(DIATHERMIC_COEFFICIENT).getDescriptor(true), false));
		flags.add(new Flag(NumericPropertyKeyword.LASER_ABSORPTIVITY, NumericProperty.def(LASER_ABSORPTIVITY).getDescriptor(true), false));
		flags.add(new Flag(NumericPropertyKeyword.THERMAL_ABSORPTIVITY, NumericProperty.def(THERMAL_ABSORPTIVITY).getDescriptor(true), false));
		flags.add(new Flag(NumericPropertyKeyword.START_TIME, NumericProperty.def(START_TIME).getDescriptor(true), false));
		return flags;
	}
	
	/**
	 * Returns the type of this {@code Flag}.
	 * @return a {@code NumericPropertyKeyword} representing the type of this {@code Flag}.
	 */

	public NumericPropertyKeyword getType() {
		return index;
	}
	
	/**
	 * Creates a new {@code Flag} object based on this {@code Flag}, but with a different {@code value}.
	 * @param value either {@code true} or {@code false}
	 * @return a {@code Flag} that replicates the {@code type} and {@code abbreviation} of this {@code Flag}, but sets a new {@code value}
	 */
	
	public Flag derive(boolean value) {
		return new Flag(this.index, this.descriptor, value);
	}
		
	/**
	 * Creates a short description for the GUI.
	 */
	
	@Override
	public String getDescriptor(boolean addHtmlTags) {
		return addHtmlTags ? "<html><b>Search for </b>" + descriptor + "</html>" :
			"<b>Search for </b>" + descriptor;
	}

	/**
	 * The value for this {@code Property} is a {@code boolean}. 
	 */
	
	@Override
	public Object getValue() {
		return value;
	}

	/**
	 * Attempts to set the value of this {@code flag} to {@code value}. 
	 * @param value a {@code boolean}
	 * @throws IllegalArgumentException If the {@code value} is not a {@code boolean}
	 */
	
	public void setValue(Object value) throws IllegalArgumentException {
		if(! (value instanceof Boolean))
			throw new IllegalArgumentException("Illegal argument: " + value);
		this.value = (boolean) value;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + index.name();
	}

	public String abbreviation(boolean addHtmlTags) {
		return addHtmlTags ? "<html>" + descriptor + "</html>" : descriptor;
	}

	public void setAbbreviation(String abbreviation) {
		this.descriptor = abbreviation;
	}

}
