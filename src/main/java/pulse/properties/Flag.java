package pulse.properties;

import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericPropertyKeyword.BASELINE_INTERCEPT;
import static pulse.properties.NumericPropertyKeyword.BASELINE_SLOPE;
import static pulse.properties.NumericPropertyKeyword.DIATHERMIC_COEFFICIENT;
import static pulse.properties.NumericPropertyKeyword.DIFFUSIVITY;
import static pulse.properties.NumericPropertyKeyword.FOV_INNER;
import static pulse.properties.NumericPropertyKeyword.FOV_OUTER;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS_SIDE;
import static pulse.properties.NumericPropertyKeyword.LASER_ABSORPTIVITY;
import static pulse.properties.NumericPropertyKeyword.LOWER_BOUND;
import static pulse.properties.NumericPropertyKeyword.MAXTEMP;
import static pulse.properties.NumericPropertyKeyword.OPTICAL_THICKNESS;
import static pulse.properties.NumericPropertyKeyword.PLANCK_NUMBER;
import static pulse.properties.NumericPropertyKeyword.SCATTERING_ALBEDO;
import static pulse.properties.NumericPropertyKeyword.SCATTERING_ANISOTROPY;
import static pulse.properties.NumericPropertyKeyword.SPOT_DIAMETER;
import static pulse.properties.NumericPropertyKeyword.THERMAL_ABSORPTIVITY;
import static pulse.properties.NumericPropertyKeyword.TIME_SHIFT;
import static pulse.properties.NumericPropertyKeyword.UPPER_BOUND;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A {@code Flag} is a {@code Property} that has a {@code type} represented by a
 * {@code NumericPropertyKeyword}, a {@code boolean value}, and a {@code String}
 * with a short abbreviation describing the type of this flag (this is usually
 * defined by the corresponding {@code NumericProperty}).
 */

public class Flag implements Property {

	private NumericPropertyKeyword index;
	private boolean value;
	private String descriptor;

	/**
	 * Creates a {@code Flag} with the type {@code type}. The default {@code value}
	 * is set to {@code false}.
	 * 
	 * @param type the {@code NumericPropertyKeyword} associated with this
	 *             {@code Flag}
	 */

	public Flag(NumericPropertyKeyword type) {
		this.index = type;
		value = false;
	}

	/**
	 * Creates a {@code Flag} with the following pre-specified parameters: type
	 * {@code type}, short description {@code abbreviations}, and {@code value}.
	 * 
	 * @param type         the {@code NumericPropertyKeyword} associated with this
	 *                     {@code Flag}
	 * @param abbreviation an abbreviation (short description, usually symbolic
	 *                     only) associated with this {@code type}
	 * @param value        the {@code boolean} value of this {@code flag}
	 */

	public Flag(NumericPropertyKeyword type, String abbreviation, boolean value) {
		this.index = type;
		this.descriptor = abbreviation;
		this.value = value;
	}

	/**
	 * A static method for converting enabled flags to a {@code List} of
	 * {@code NumericPropertyKeyword}s. Each keyword in this list corresponds to an
	 * enabled flag in the {@code flags} {@code List}.
	 * 
	 * @param flags the list of flags that needs to be analysed
	 * @return a list of {@code NumericPropertyKeyword}s corresponding to enabled
	 *         {@code flag}s.
	 */

	public static List<NumericPropertyKeyword> convert(List<Flag> flags) {
		List<Flag> filtered = flags.stream().filter(flag -> (boolean) flag.getValue()).collect(Collectors.toList());
		return filtered.stream().map(flag -> flag.getType()).collect(Collectors.toList());
	}

	/**
	 * The default list of {@code Flag}s used in finding the reverse solution of the
	 * heat conduction problem contains:
	 * <code>DIFFUSIVITY (true), HEAT_LOSS (true), MAXTEMP (true), BASELINE_INTERCEPT (false), BASELINE_SLOPE (false) </code>.
	 * 
	 * @return a {@code List} of default {@code Flag}s
	 */

	public static List<Flag> allProblemDependentFlags() {
		List<Flag> flags = new ArrayList<>();
		flags.add(new Flag(DIFFUSIVITY, def(DIFFUSIVITY).getDescriptor(true), true));
		flags.add(new Flag(HEAT_LOSS, def(HEAT_LOSS).getDescriptor(true), true));
		flags.add(new Flag(HEAT_LOSS_SIDE, def(HEAT_LOSS_SIDE).getDescriptor(true), true));
		flags.add(new Flag(MAXTEMP, def(MAXTEMP).getDescriptor(true), true));
		flags.add(new Flag(FOV_OUTER, def(FOV_OUTER).getDescriptor(true), true));
		flags.add(new Flag(FOV_INNER, def(FOV_INNER).getDescriptor(true), true));
		flags.add(new Flag(SPOT_DIAMETER, def(SPOT_DIAMETER).getDescriptor(true), true));
		flags.add(new Flag(DIATHERMIC_COEFFICIENT, def(DIATHERMIC_COEFFICIENT).getDescriptor(true), true));
		flags.add(new Flag(LASER_ABSORPTIVITY, def(LASER_ABSORPTIVITY).getDescriptor(true), true));
		flags.add(new Flag(THERMAL_ABSORPTIVITY, def(THERMAL_ABSORPTIVITY).getDescriptor(true), false));
		flags.add(new Flag(OPTICAL_THICKNESS, def(OPTICAL_THICKNESS).getDescriptor(true), true));
		flags.add(new Flag(PLANCK_NUMBER, def(PLANCK_NUMBER).getDescriptor(true), true));
		flags.add(new Flag(SCATTERING_ALBEDO, def(SCATTERING_ALBEDO).getDescriptor(true), true));
		flags.add(new Flag(SCATTERING_ANISOTROPY, def(SCATTERING_ANISOTROPY).getDescriptor(true), true));
		flags.add(new Flag(BASELINE_INTERCEPT, def(BASELINE_INTERCEPT).getDescriptor(true), false));
		flags.add(new Flag(BASELINE_SLOPE, def(BASELINE_SLOPE).getDescriptor(true), false));
		return flags;
	}

	public static List<Flag> allProblemIndependentFlags() {
		List<Flag> flags = new ArrayList<>();
		flags.add(new Flag(TIME_SHIFT, def(TIME_SHIFT).getDescriptor(true), false));
		flags.add(new Flag(LOWER_BOUND, def(LOWER_BOUND).getDescriptor(true), false));
		flags.add(new Flag(UPPER_BOUND, def(UPPER_BOUND).getDescriptor(true), false));
		return flags;
	}

	/**
	 * Returns the type of this {@code Flag}.
	 * 
	 * @return a {@code NumericPropertyKeyword} representing the type of this
	 *         {@code Flag}.
	 */

	public NumericPropertyKeyword getType() {
		return index;
	}

	/**
	 * Creates a new {@code Flag} object based on this {@code Flag}, but with a
	 * different {@code value}.
	 * 
	 * @param value either {@code true} or {@code false}
	 * @return a {@code Flag} that replicates the {@code type} and
	 *         {@code abbreviation} of this {@code Flag}, but sets a new
	 *         {@code value}
	 */

	public Flag derive(boolean value) {
		return new Flag(this.index, this.descriptor, value);
	}

	/**
	 * Creates a short description for the GUI.
	 */

	@Override
	public String getDescriptor(boolean addHtmlTags) {
		return addHtmlTags ? "<html><b>Search for </b>" + descriptor + "</html>" : "<b>Search for </b>" + descriptor;
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
	 * 
	 * @param value a {@code boolean}
	 * @throws IllegalArgumentException If the {@code value} is not a
	 *                                  {@code boolean}
	 */

	public void setValue(Object value) throws IllegalArgumentException {
		if (!(value instanceof Boolean))
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

	@Override
	public boolean attemptUpdate(Object value) {
		// TODO Auto-generated method stub
		return false;
	}

	public static List<Flag> selectActive(List<Flag> flags) {
		return flags.stream().filter(flag -> (boolean) flag.getValue()).collect(Collectors.toList());
	}

}