package pulse.properties;

import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import pulse.math.Segment;
import static pulse.properties.NumericProperties.compare;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperties.isValueSensible;
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
    private NumericPropertyKeyword[] excludes;

    private boolean autoAdjustable;
    private boolean discrete;
    private boolean defaultSearchVariable;
    private boolean optimisable;

    /**
     * Creates a {@code NumericProperty} based on {
     *
     * @pattern} and assigns {@code value} as its value.
     *
     * @param value the {@code} for the {
     * @NumericProperty} that is otherwise equal to {@code pattern}
     * @param pattern a valid {@code NumericProperty}
     */
    protected NumericProperty(Number value, NumericProperty pattern) {
        this(pattern);
        this.value = value;
    }

    /**
     * Constructor used by {@code XMLConverter} to create a
     * {@code NumericProperty} with fully specified set of parameters
     *
     * @param type the type of this {@code NumericProperty}, set by one of the
     * {@code NumericPropertyKeyword} constants
     * @param params	the numeric parameters in the following order: value,
     * minimum, maximum, dimension factor.
     * @see pulse.io.export.XMLConverter
     */
    public NumericProperty(NumericPropertyKeyword type, Number... params) {
        if (params.length != 4) {
            throw new IllegalArgumentException("Input array must be of length 4. Received: " + params.length);
        }

        this.type = type;
        this.value = params[0];
        this.dimensionFactor = params[3];
        this.excludes = new NumericPropertyKeyword[0];
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
        this.discrete = num.discrete;
        this.dimensionFactor = num.dimensionFactor;
        this.autoAdjustable = num.autoAdjustable;
        this.error = num.error;
        this.defaultSearchVariable = num.defaultSearchVariable;
        this.optimisable = num.optimisable;
        this.excludes = num.excludes;
    }

    public NumericPropertyKeyword[] getExcludeKeywords() {
        return excludes;
    }

    public void setExcludeKeywords(NumericPropertyKeyword[] keys) {
        this.excludes = keys;
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
     * Sets the {@code value} of this {@code NumericProperty} -- if and only if
     * the new {@code value} is confined within the definition domain for this
     * {@code NumericProperty}. Checks whether
     *
     * @param value the value to be set to {@code this property}
     * @see NumericProperties.isValueSensible(NumericProperty,Number)
     */
    public void setValue(Number value) {

        Number oldValue = this.value;
        this.value = value;

        if (!validate()) {
            this.value = oldValue;
            throw new IllegalArgumentException(printRangeAndNumber(this, value));
        }

    }

    /**
     * Sets the definition domain for this {@code NumericProperty}.
     *
     * @param minimum the minimum value
     * @param maximum the maximum value
     * @throws IllegalArgumentException if any two of
     * {@code minimum, maximum, or this.value} have different primitive types
     * (e.g. a {@code double} and an {@code int}).
     */
    public void setDomain(Number minimum, Number maximum) throws IllegalArgumentException {
        var minClass = minimum.getClass();
        var maxClass = maximum.getClass();
        if (!minClass.equals(maxClass)) {
            throw new IllegalArgumentException(
                    "Types of minimum and maximum do not match: " + minClass + " and " + maxClass);
        }
        if (!minClass.equals(value.getClass())) {
            throw new IllegalArgumentException("Interrupted attempt of setting " + minClass.getSimpleName()
                    + " boundaries to a " + value.getClass().getSimpleName() + " property");
        }
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
        return type + " = " + formattedOutput();
    }

    public Number valueInCurrentUnits() {
        return value instanceof Double ? (double) value * dimensionFactor.doubleValue()
                + getDimensionDelta().doubleValue() : (int) value;
    }

    public Number errorInCurrentUnits() {
        return error == null ? 0.0 : error.doubleValue() * dimensionFactor.doubleValue();
    }

    public Number getDimensionFactor() {
        return dimensionFactor;
    }

    public void setDimensionFactor(Number dimensionFactor) {
        this.dimensionFactor = dimensionFactor;
    }

    public void setVisibleByDefault(boolean autoAdjustable) {
        this.autoAdjustable = autoAdjustable;
    }

    public boolean isVisibleByDefault() {
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
     * {@code NumericProperty} if (a) it is of the same class; (b) its value is
     * the same as for this {@code NumericProperty}, and (c) if it is specified
     * by the same {@code NumericPropertyKeyword}.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o == this) {
            return true;
        }

        if (!(o instanceof NumericProperty)) {
            return false;
        }

        NumericProperty onp = (NumericProperty) o;

        if (onp.getType() != this.getType()) {
            return false;
        }

        return compare(this, onp) == 0;

    }

    @Override
    public int compareTo(NumericProperty arg0) {
        final int result = this.getType().compareTo(arg0.getType());
        return result != 0 ? result : compare(this, arg0);
    }

    public boolean isDiscrete() {
        return discrete;
    }

    public void setDiscrete(boolean discrete) {
        this.discrete = discrete;
    }

    @Override
    public boolean attemptUpdate(Object value) {
        if (!(value instanceof Number)) {
            return false;
        }

        if (!(derive(this.getType(), (Number) value).validate())) {
            return false;
        }

        this.value = (Number) value;
        return true;

    }

    public static void requireType(NumericProperty property, NumericPropertyKeyword type) {
        if (property.getType() != type) {
            throw new IllegalArgumentException("Illegal type: " + property.getType());
        }
    }

    public boolean isDefaultSearchVariable() {
        return defaultSearchVariable;
    }

    public boolean isOptimisable() {
        return optimisable;
    }

    public void setDefaultSearchVariable(boolean defaultSearchVariable) {
        this.defaultSearchVariable = defaultSearchVariable;
    }

    public void setOptimisable(boolean optimisable) {
        this.optimisable = optimisable;
    }
    
    public Number getDimensionDelta() {
        if(type == NumericPropertyKeyword.TEST_TEMPERATURE)
            return -273.15;
        else
            return 0.0;
    }
    
    /**
     * Represents the bounds specified for this numeric property 
     * as a {@code Segment} object. The bound numbers are taken by
     * their double values and assigned to the segment.
     * @return the bounds in which this property is allowed to change
     */
    
    public Segment getBounds() {
        return new Segment(minimum.doubleValue(), maximum.doubleValue());
    }
    
    /**
     * Uses a {@code NumericPropertyFormatter} to generate a formatted output
     * @return a formatted string output with the value (and error -- if available)
     * of this numeric property
     */
    
    public String formattedOutput() {
        var num = new NumericPropertyFormatter(this, true, true);
        return num.numberFormat(this).format(value);
    }
    
}