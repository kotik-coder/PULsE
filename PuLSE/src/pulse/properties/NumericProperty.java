package pulse.properties;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import pulse.ui.Messages;
import pulse.util.XMLConverter;

/**
 * A {@code Property} that has a numeric {@code value}, an {@code error} 
 * associated with this value, a definition domain defining sensible value,
 * and a {@code dimensionFactor} used to convert the value to SI. <p> The description,
 * abbreviation, and all default values for a specific {@code NumericProperty}
 * are associated with the specific type set out by the {@code NuemricPropertyKeyword}.
 * The latter is used to link with a repository of default {@code NumericPropert}ies
 * loaded from an {@code .xml} file.</p>
 * @see pulse.properties.NumericPropertyKeyword
 * @see pulse.util.XMLConverter
 */

public class NumericProperty implements Property, Comparable<NumericProperty> {

	private Number value;
	private Number minimum, maximum;
	private String descriptor;
	private String abbreviation;
	private Number dimensionFactor;
	private Number error;
	private NumericPropertyKeyword type;
	private boolean autoAdjustable = true;
	
	/**
	 * The list of default properties read that is created by reading the default {@code .xml} file.
	 */
	
	private final static List<NumericProperty> DEFAULT = XMLConverter.readDefaultXML();
	
	/**
	 * Creates a {@code NumericProperty} based on {@pattern} 
	 * and assigns {@code value} as its value.
	 * @param value the {@code} for the {@NumericProperty} that is otherwise equal to {@code pattern}
	 * @param pattern a valid {@code NumericProperty} 
	 */
	
	public NumericProperty(Number value, NumericProperty pattern) {
		this(pattern);
		this.value = value;
	}
	
	/**
	 * Constructor used by {@code XMLConverter} to create a {@code NumericProperty}
	 * with fully specified set of parameters
	 * @param type the type of this {@code NumericProperty}, set by one of the {@code NumericPropertyKeyword} constants
	 * @param descriptor a {@code String} with full description of the property (may use html-formatting, but without the 'html' tags!)
	 * @param abbreviation a {@code String} with a symbolic representation of the property (may use html-formatting, but without the 'html' tags!)
	 * @param value a numeric value, which can be either a {@code Double} or an {@code Integer} (or primitive types)
	 * @param minimum the minimum allowed value for this {@code type}
	 * @param maximum the maximum allowed value for this {@code type}
	 * @param dimensionFactor a multiplier that will be used when converting the value to SI
	 * @param autoAdjustable a boolean flag indicating if the property requires user input
	 * @see pulse.util.XMLConverter
	 */
	
	public NumericProperty(NumericPropertyKeyword type, String descriptor, 
			String abbreviation, Number value, Number minimum, 
		   Number maximum, Number dimensionFactor, boolean autoAdjustable) {		
		this.type = type;
		this.descriptor = descriptor;
		this.abbreviation = abbreviation;
		this.value = value;
		this.dimensionFactor = dimensionFactor; 
		this.autoAdjustable = autoAdjustable;
		setDomain(minimum, maximum);
	}
	
	/**
	 * A copy constructor for {@code NumericProperty}
	 * @param num another {@code NumericProperty} that is going to be replicated
	 */
		
	public NumericProperty(NumericProperty num) {
		this.value 	 = num.value;
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
	
	/**
	 * Checks whether the {@code val} that is going to be passed to the {@code property}
	 * (a) has the same type as the {@code property.getValue()} object; 
	 * (b) is confined within the definition domain: {@code minimum <= value <= maximum}.
	 * Called within {@code setValue(Number)} method.
	 * @param property the {@code property} containing the definition domain
	 * @param val a numeric value, the conformity of which to the definition domain needs to be checked
	 * @return {@code true} if {@code minimum <= val <= maximum} and if both {@code val} and {@code value}
	 * are instances of the same {@code class}; {@code false} otherwise
	 * @see setValue(Number)
	 */
	
	public static boolean isValueSensible(NumericProperty property, Number val) {				
		if(!property.value.getClass().equals(val.getClass()))
			return false;
		
		double v = val.doubleValue();
		
		double max = property.getMaximum().doubleValue();

		final double EPS = 1E-10;
		
		if( v > max + EPS ) 
			return false;
		
		double min = property.getMinimum().doubleValue();
		
		if( v < min - EPS )
			return false;

		return true;
		
	}
	
	/**
	 * Sets the {@code value} of this {@code NumericProperty} -- if and only if
	 * the new {@code value} is confined within the definition domain for this
	 * {@code NumericProperty}. Checks whether 
	 * @param value the value to be set to {@code this property}
	 * @see isValueSensible(NumericProperty,Number)
	 */

	public void setValue(Number value) {
		
		if( ! NumericProperty.isValueSensible(this, value) ) {
			StringBuilder msg = new StringBuilder();
			msg.append("Allowed range for ");
			msg.append(type + " : ");
			msg.append(this.value.getClass().getSimpleName()); 
			msg.append(" from " + minimum);
			msg.append(" to " + maximum);
			msg.append(". Received value: " + value); 
			throw new IllegalArgumentException(msg.toString());
		}

		this.value = value;
		
	}
	
	/**
	 * Sets the definition domain for this {@code NumericProperty}. 
	 * @param minimum the minimum value
	 * @param maximum the maximum value
	 * @throws IllegalArgumentException if any two of {@code minimum, maximum, or this.value} have 
	 * different primitive types (e.g. a {@code double} and an {@code int}).
	 */
	
	public void setDomain(Number minimum, Number maximum) throws IllegalArgumentException {
		Class<? extends Number> minClass = minimum.getClass();
		Class<? extends Number> maxClass = maximum.getClass();
		if(! minClass.equals(maxClass))
				throw new IllegalArgumentException("Types of minimum and maximum do not match: " + minClass + " and " + maxClass); 
		if(! minClass.equals(value.getClass()))
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
	 * Prints out the {@code type} and {@code value} of this {@code NumericProperty}.
	 */
	
	@Override
	public String toString() {
		return(type + " = " + formattedValue(false));
	}
	
	/**
	 * Calls {@code formattedValue(true)}.
	 * @see formattedValue(boolean)
	 */
	
	@Override
	public String formattedValue() {
		return this.formattedValue(true);
	}		
	
	/**
	 * Used to print out a nice {@code value} for GUI applications and for exporting.
	 * <p>Will use a {@code DecimalFormat} to reduce the number of digits, if neccessary.
	 * Automatically detects whether it is dealing with {@code int} or {@code double} values,
	 * and adjust formatting accordingly. If {@code error != null}, will use the latter as 
	 * the error value, which is separated from the main value by a plus-minus sign.</p>
	 * @param convertDimension if {@code true}, the output will be the {@code value * dimensionFactor}
	 * @return a nice {@code String} representing the {@code value} of this {@code NumericProperty} and its {@code error}
	 */
	
	public String formattedValue(boolean convertDimension) {
		
		if(value instanceof Integer) { 
			Number val = convertDimension ? 
					value.intValue() * dimensionFactor.intValue() :
					value.intValue();
			return (NumberFormat.getIntegerInstance()).format(val);
		}
		
		final String PLUS_MINUS = Messages.getString("NumericProperty.PlusMinus"); 
		
		final double UPPER_LIMIT = 1e4; 	//the upper limit, used for formatting
		final double LOWER_LIMIT = 1e-2;	//the lower limit, used for formatting
		final double ZERO		 = 1e-30;
		
		double adjustedValue = convertDimension ? 
			(double) value * this.getDimensionFactor().doubleValue() : 
			(double) value;
			
		double absAdjustedValue = Math.abs(adjustedValue);
		
		DecimalFormat selectedFormat = null;
		
		if( (absAdjustedValue > UPPER_LIMIT) || (absAdjustedValue < LOWER_LIMIT && absAdjustedValue > ZERO) )
			selectedFormat = new DecimalFormat(Messages.getString("NumericProperty.BigNumberFormat"));
		else
			selectedFormat = new DecimalFormat(Messages.getString("NumericProperty.NumberFormat"));
		
		if(error != null)
			return selectedFormat.format(adjustedValue) 
				+ PLUS_MINUS 
				+ selectedFormat.format( convertDimension ? (double) error*getDimensionFactor().doubleValue() :
															(double) error );
		else
			return selectedFormat.format(adjustedValue);
			
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
	 * Searches for the default {@code NumericProperty} corresponding to {@code keyword}
	 * in the list of pre-defined properties loaded from the respective {@code .xml} file,
	 * and if found creates a new {@NumericProperty} which will replicate all field of 
	 * the latter, but will set its value to {@code value}.
	 * @param keyword one of the constant {@code NumericPropertyKeyword}s
	 * @param value the new value for the created {@code NumericProperty}
	 * @return a new {@code NumericProperty} that is built according to the default pattern
	 * specified by the {@code keyword}, but with a different {@code value} 
	 * @see pulse.properties.NumericPropertyKeyword
	 */
	
	public static NumericProperty derive(NumericPropertyKeyword keyword, Number value) {
		return new NumericProperty(
				value, DEFAULT.stream().filter(p -> p.getType() == keyword).findFirst().get());
	}
	
	/**
	 * Searches for the default {@code NumericProperty} corresponding to {@code keyword}
	 * in the list of pre-defined properties loaded from the respective {@code .xml} file.
	 * @param keyword one of the constant {@code NumericPropertyKeyword}s 
	 * @return a new {@code NumericProperty} that is built according to the default pattern
	 * specified by the {@code keyword} 
	 * @see pulse.properties.NumericPropertyKeyword
	 */
	
	public static NumericProperty def(NumericPropertyKeyword keyword) {
		return new NumericProperty(
				DEFAULT.stream().filter(p -> p.getType() == keyword).findFirst().get());
	}
	
	/**
	 * The {@code Object} o is considered to be equal to this {@code NumericProperty} 
	 * if (a) it is of the same class; (b) its value is the same as for this {@code NumericProperty},
	 * and (c) if it is specified by the same {@code NumericPropertyKeyword}. 
	 */
	
	@Override
	public boolean equals(Object o) {
		if(o == this)
	        return true;
		
		if(! (o instanceof NumericProperty) )
			return false;

		NumericProperty onp = (NumericProperty) o;
		
		if(onp.getType() != this.getType())
			return false;
		
		if(!onp.getValue().equals(this.getValue()))
			return false;
		
		return true;
		
	}
	
	@Override
	public int compareTo(NumericProperty arg0) {
		int result = this.getType().compareTo(arg0.getType());
		
		if(result != 0) 
			return result;
		
		return compareValues(arg0);					
		
	}
	
	/**
	 * Compares the numeric values of this {@code NumericProperty} and {@code arg0}
	 * @param arg0 another {@code NumericProperty}.
	 * @return {@code true} if the values are equals
	 */
	
	public int compareValues(NumericProperty arg0) {
		Double d1 = ((Number)value).doubleValue();
		Double d2 = ((Number)arg0.getValue()).doubleValue(); 
		
		return d1.compareTo(d2);
	}
	
	/**
	 * Searches for the default {@code NumericProperty} corresponding to {@code keyword}
	 * in the list of pre-defined properties loaded from the respective {@code .xml} file.
	 * Note this is different from {@code def(NumericPropertyKeyword)} as it does not create
	 * a new instance of this class.
	 * @param keyword one of the constant {@code NumericPropertyKeyword}s 
	 * @return a {@code NumericProperty} in the default list of properties 
	 * @see pulse.properties.NumericPropertyKeyword
	 * @see def(NumericPropertyKeyword)
	 */
		
	public static NumericProperty theDefault(NumericPropertyKeyword keyword) {
		return DEFAULT.stream().filter(p -> p.getType() == keyword).findFirst().get();
	}
	
}