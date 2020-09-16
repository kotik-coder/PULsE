package pulse.properties;

import static pulse.properties.NumericProperties.compare;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperties.formattedValueAndError;
import static pulse.properties.NumericProperties.isValueSensible;
import static pulse.properties.NumericProperties.numberFormat;
import static pulse.properties.NumericProperties.printRangeAndNumber;

/**
 * A {@code Property} that has a numeric {@code value}, an {@code error}
 * associated with this value, a definition domain defining sensible value, and
 * a {@code dimensionFactor} used to convert the value to SI.
 * <p>
 * The description, abbreviation, and all default values for a specific
 * {@code NumericProperty} are associated with the specific type set out by the
 * {@code NuemricPropertyKeyword}. The latter is used to link with a repository
 * of default {@code NumericPropert}ies loaded from an {@code .xml} file.
 * </p>
 * 
 * @see pulse.properties.NumericPropertyKeyword
 * @see pulse.io.export.XMLConverter
 */

public class NumericProperty implements Property, Comparable<NumericProperty> {

	private Number value;

	private Number minimum;
	private Number maximum;

	private String descriptor;
	private String abbreviation;

	private Number dimensionFactor;
	private Number error;

	private NumericPropertyKeyword type;

	private boolean autoAdjustable;
	private boolean discreet;
	private boolean defaultSearchVariable;

	/**
	 * Creates a {@code NumericProperty} based on {@pattern} and assigns
	 * {@code value} as its value.
	 * 
	 * @param value   the {@code} for the {@NumericProperty} that is otherwise equal
	 *                to {@code pattern}
	 * @param pattern a valid {@code NumericProperty}
	 */

	protected NumericProperty(Number value, NumericProperty pattern) {
		this(pattern);
		this.value = value;
	}

	/**
	 * Constructor used by {@code XMLConverter} to create a {@code NumericProperty}
	 * with fully specified set of parameters
	 * 
	 * @param type            the type of this {@code NumericProperty}, set by one
	 *                        of the {@code NumericPropertyKeyword} constants
	 * @param descriptor      a {@code String} with full description of the property
	 *                        (may use html-formatting, but without the 'html'
	 *                        tags!)
	 * @param abbreviation    a {@code String} with a symbolic representation of the
	 *                        property (may use html-formatting, but without the
	 *                        'html' tags!)
	 * @param value           a numeric value, which can be either a {@code Double}
	 *                        or an {@code Integer} (or primitive types)
	 * @param minimum         the minimum allowed value for this {@code type}
	 * @param maximum         the maximum allowed value for this {@code type}
	 * @param dimensionFactor a multiplier that will be used when converting the
	 *                        value to SI
	 * @param autoAdjustable  a boolean flag indicating if the property requires
	 *                        user input
	 * @see pulse.io.export.XMLConverter
	 */

	public NumericProperty(NumericPropertyKeyword type, Number... params) {
		if(params.length != 4)
			throw new IllegalArgumentException("Input array must be of length 4. Received: " + params.length);
		
		this.type = type;
		this.value = params[0];
		this.dimensionFactor = params[3];
		setDomain(params[1], params[2]);
	}

	/**
	 * A copy constructor for {@code NumericProperty}
	 * 
	 * @param num another {@code NumericProperty} that is going to be replicated
	 */

	public NumericProperty(NumericProperty num) {
		this.value = num.value;
		this.descriptor = num.descriptor;
		this.abbreviation = num.abbreviation;
		this.minimum = num.minimum;
		this.maximum = num.maximum;
		this.type = num.type;
		this.dimensionFactor = num.dimensionFactor;
		this.autoAdjustable = num.autoAdjustable;
	}

	public NumericPropertyKeyword getType() {
		return type;
	}

	@Override
	public Object getValue() {
		return value;
	}

	public boolean validate() {
		return isValueSensible(this, value);
	}

	/**
	 * Sets the {@code value} of this {@code NumericProperty} -- if and only if the
	 * new {@code value} is confined within the definition domain for this
	 * {@code NumericProperty}. Checks whether
	 * 
	 * @param value the value to be set to {@code this property}
	 * @see isValueSensible(NumericProperty,Number)
	 */

	public void setValue(Number value) {

		if (!validate())
			throw new IllegalArgumentException(printRangeAndNumber(this, value));

		this.value = value;

	}

	/**
	 * Sets the definition domain for this {@code NumericProperty}.
	 * 
	 * @param minimum the minimum value
	 * @param maximum the maximum value
	 * @throws IllegalArgumentException if any two of
	 *                                  {@code minimum, maximum, or this.value} have
	 *                                  different primitive types (e.g. a
	 *                                  {@code double} and an {@code int}).
	 */

	public void setDomain(Number minimum, Number maximum) throws IllegalArgumentException {
		var minClass = minimum.getClass();
		var maxClass = maximum.getClass();
		if (!minClass.equals(maxClass))
			throw new IllegalArgumentException(
					"Types of minimum and maximum do not match: " + minClass + " and " + maxClass);
		if (!minClass.equals(value.getClass()))
			throw new IllegalArgumentException("Interrupted attempt of setting " + minClass.getSimpleName()
					+ " boundaries to a " + value.getClass().getSimpleName() + " property");
		this.minimum = minimum;
		this.maximum = maximum;
	}

	public Number getMinimum() {
		return minimum;
	}

	public Number getMaximum() {
		return maximum;
	}

	/**
	 * Prints out the {@code type} and {@code value} of this
	 * {@code NumericProperty}.
	 */

	@Override
	public String toString() {
		return (type + " = " + formattedValueAndError(this, false));
	}

	/**
	 * Calls {@code formattedValue(true)}.
	 * 
	 * @see formattedValueAndError(boolean)
	 */

	@Override
	public String formattedOutput() {
		return formattedValueAndError(this, true);
	}

	public String valueOutput() {
		return numberFormat(this, true).format(valueInCurrentUnits());
	}

	public String errorOutput() {
		return numberFormat(this, true).format(errorInCurrentUnits());
	}

	public Number valueInCurrentUnits() {
		return value instanceof Double ? (double) value * dimensionFactor.doubleValue() : (int) value;
	}

	public double errorInCurrentUnits() {
		return error == null ? 0.0 : (double) error * dimensionFactor.doubleValue();
	}

	public Number getDimensionFactor() {
		return dimensionFactor;
	}

	public void setDimensionFactor(Number dimensionFactor) {
		this.dimensionFactor = dimensionFactor;
	}

	public void setAutoAdjustable(boolean autoAdjustable) {
		this.autoAdjustable = autoAdjustable;
	}

	public boolean isAutoAdjustable() {
		return autoAdjustable;
	}

	public Number getError() {
		return error;
	}

	public void setError(Number error) {
		this.error = error;
	}

	@Override
	public String getDescriptor(boolean addHtmlTag) {
		return addHtmlTag ? "<html>" + descriptor + "</html>" : descriptor;
	}

	public void setDescriptor(String descriptor) {
		this.descriptor = descriptor;
	}

	public String getAbbreviation(boolean addHtmlTags) {
		return addHtmlTags ? "<html>" + abbreviation + "</html>" : abbreviation;
	}

	public void setAbbreviation(String abbreviation) {
		this.abbreviation = abbreviation;
	}

	/**
	 * The {@code Object} o is considered to be equal to this
	 * {@code NumericProperty} if (a) it is of the same class; (b) its value is the
	 * same as for this {@code NumericProperty}, and (c) if it is specified by the
	 * same {@code NumericPropertyKeyword}.
	 */

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;

		if (o == this)
			return true;

		if (!(o instanceof NumericProperty))
			return false;

		NumericProperty onp = (NumericProperty) o;

		if (onp.getType() != this.getType())
			return false;

		return compare(this, onp) == 0;

	}

	@Override
	public int compareTo(NumericProperty arg0) {
		final int result = this.getType().compareTo(arg0.getType());
		return result != 0 ? result : compare(this, arg0);
	}

	public boolean isDiscrete() {
		return discreet;
	}

	public void setDiscreet(boolean discreet) {
		this.discreet = discreet;
	}

	@Override
	public boolean attemptUpdate(Object value) {
		if (!(value instanceof Number))
			return false;

		if (!(derive(this.getType(), (Number) value).validate()))
			return false;

		this.value = (Number) value;
		return true;

	}

	public static void requireType(NumericProperty property, NumericPropertyKeyword type) {
		if (property.getType() != type)
			throw new IllegalArgumentException("Illegal type: " + property.getType());
	}

	public boolean isDefaultSearchVariable() {
		return defaultSearchVariable;
	}
	
	public void setDefaultSearchVariable(boolean defaultSearchVariable) {
		this.defaultSearchVariable = defaultSearchVariable;
	}

}